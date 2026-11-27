package com.mortgagetracker.entry;

import com.mortgagetracker.entry.XlsxEntryParser.ParseResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class XlsxEntryParserTest {

    @Test
    void parsesTextAndExcelDatesAndStopsAtFirstEmptyRow() throws Exception {
        byte[] file;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet();
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd/mm/yyyy"));

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Date");
            header.createCell(1).setCellValue("Total");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("01/02/2025");
            r1.createCell(1).setCellValue(1169.18);
            r1.createCell(2).setCellValue(833.33);
            r1.createCell(3).setCellValue(335.85);

            Row r2 = sheet.createRow(2);
            Cell d = r2.createCell(0);
            d.setCellValue(LocalDate.of(2025, 3, 1));
            d.setCellStyle(dateStyle);
            r2.createCell(1).setCellValue("£5,000.00");
            // C empty -> 0
            r2.createCell(3).setCellFormula("2500*2");

            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("31/02/2025");
            r3.createCell(1).setCellValue(10);

            Row r4 = sheet.createRow(4);
            r4.createCell(0).setCellValue("01/04/2025");
            r4.createCell(1).setCellValue("abc");

            // row 6 empty, row 7 must be ignored
            Row r7 = sheet.createRow(6);
            r7.createCell(0).setCellValue("01/05/2025");

            wb.write(out);
            file = out.toByteArray();
        }

        ParseResult result = new XlsxEntryParser().parse(new ByteArrayInputStream(file));

        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().get(0).date()).isEqualTo(LocalDate.of(2025, 2, 1));
        assertThat(result.rows().get(0).total()).isEqualByComparingTo("1169.18");
        assertThat(result.rows().get(1).date()).isEqualTo(LocalDate.of(2025, 3, 1));
        assertThat(result.rows().get(1).total()).isEqualByComparingTo("5000");
        assertThat(result.rows().get(1).rent()).isEqualByComparingTo("0");
        assertThat(result.rows().get(1).equity()).isEqualByComparingTo("5000");

        assertThat(result.errors()).extracting(e -> e.row()).containsExactly(4, 5);
    }
}
