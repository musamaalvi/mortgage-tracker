package com.mortgagetracker.entry;

import com.mortgagetracker.entry.EntryDtos.RowError;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads payment rows from the first sheet of a workbook, starting at row 2:
 * A = date (Excel date or dd/mm/yyyy text), B = total payment, C = rent, D = equity.
 * Parsing stops at the first row where A–D are all empty.
 */
class XlsxEntryParser {

    public record ParsedRow(int row, LocalDate date, BigDecimal total, BigDecimal rent, BigDecimal equity) {}

    public record ParseResult(List<ParsedRow> rows, List<RowError> errors) {}

    private static final DateTimeFormatter UK_DATE =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    public ParseResult parse(InputStream in) throws IOException {
        List<ParsedRow> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isEmpty(row)) {
                    break;
                }
                int rowNumber = i + 1; // as shown in Excel
                try {
                    rows.add(new ParsedRow(rowNumber,
                            readDate(row.getCell(0), evaluator),
                            readAmount(row.getCell(1), evaluator, "B"),
                            readAmount(row.getCell(2), evaluator, "C"),
                            readAmount(row.getCell(3), evaluator, "D")));
                } catch (IllegalArgumentException e) {
                    errors.add(new RowError(rowNumber, e.getMessage()));
                }
            }
        }
        return new ParseResult(rows, errors);
    }

    private static boolean isEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = 0; c < 4; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !(cell.getCellType() == CellType.STRING && cell.getStringCellValue().isBlank())) {
                return false;
            }
        }
        return true;
    }

    private static LocalDate readDate(Cell cell, FormulaEvaluator evaluator) {
        CellValue value = cell == null ? null : evaluator.evaluate(cell);
        if (value == null || value.getCellType() == CellType.BLANK) {
            throw new IllegalArgumentException("Date (column A) is empty");
        }
        if (value.getCellType() == CellType.NUMERIC) {
            // An Excel date is a serial number; the cell's display format doesn't matter here.
            return DateUtil.getLocalDateTime(value.getNumberValue()).toLocalDate();
        }
        if (value.getCellType() == CellType.STRING) {
            String text = value.getStringValue().trim();
            try {
                return LocalDate.parse(text, UK_DATE);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Date '" + text + "' is not in dd/mm/yyyy format");
            }
        }
        throw new IllegalArgumentException("Date (column A) is not a date");
    }

    private static BigDecimal readAmount(Cell cell, FormulaEvaluator evaluator, String column) {
        CellValue value = cell == null ? null : evaluator.evaluate(cell);
        if (value == null || value.getCellType() == CellType.BLANK) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal amount;
        if (value.getCellType() == CellType.NUMERIC) {
            amount = BigDecimal.valueOf(value.getNumberValue());
        } else if (value.getCellType() == CellType.STRING) {
            String text = value.getStringValue().replaceAll("[£,\\s]", "");
            if (text.isEmpty()) {
                return BigDecimal.ZERO.setScale(2);
            }
            try {
                amount = new BigDecimal(text);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Column " + column + " value '" + value.getStringValue() + "' is not a number");
            }
        } else {
            throw new IllegalArgumentException("Column " + column + " is not a number");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Column " + column + " is negative");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
