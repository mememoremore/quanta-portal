package com.ruoyi.qt.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import com.ruoyi.qt.domain.QtLabMemberImportRow;

public class QtLabMemberImportParserTest
{
    @Test
    public void normalizesDepartmentAndMemberFlag()
    {
        assertEquals("FRONTEND", QtLabMemberImportParser.normalizeDepartment("\u524d\u7aef"));
        assertEquals("BACKEND", QtLabMemberImportParser.normalizeDepartment("\u540e\u7aef"));
        assertEquals("PRODUCT", QtLabMemberImportParser.normalizeDepartment("PRODUCT"));
        assertEquals("1", QtLabMemberImportParser.normalizeMemberFlag("\u662f"));
        assertEquals("1", QtLabMemberImportParser.normalizeMemberFlag("\u5854\u5458"));
        assertEquals("0", QtLabMemberImportParser.normalizeMemberFlag("\u65b0\u751f"));
        assertEquals("MGMT", QtLabMemberImportParser.resolveRoleCategory("\u8d1f\u8d23\u4eba"));
        assertEquals("MGMT", QtLabMemberImportParser.resolveRoleCategory("\u4ea7\u54c1vp"));
        assertEquals("MANAGER", QtLabMemberImportParser.resolveRoleCategory("\u7ecf\u7406"));
        assertEquals("INTERN", QtLabMemberImportParser.resolveRoleCategory("\u5b9e\u4e60\u751f"));
    }

    @Test
    public void parsesFlexibleHeaders() throws Exception
    {
        try (XSSFWorkbook workbook = new XSSFWorkbook())
        {
            XSSFSheet sheet = workbook.createSheet("\u6210\u5458\u540d\u5355");
            XSSFRow header = sheet.createRow(0);
            String[] titles = {
                    "\u767b\u5f55\u540d\u79f0(\u8fd9\u4e2a\u5fc5\u987b\u552f\u4e00)", "\u7528\u6237\u540d\u79f0", "\u7528\u6237\u90ae\u7bb1",
                    "\u624b\u673a\u53f7\u7801", "\u7528\u6237\u6027\u522b", "\u6210\u5458\u90e8\u95e8", "\u6210\u5458\u804c\u79f0",
                    "\u6210\u5458\u5c4a\u6b21", "\u5b66\u53f7", "\u73ed\u7ea7", "\u662f\u5426\u5854\u5458", "\u4e13\u4e1a"
            };
            for (int i = 0; i < titles.length; i++)
            {
                header.createCell(i).setCellValue(titles[i]);
            }
            XSSFRow data = sheet.createRow(1);
            data.createCell(0).setCellValue("wangxiaoming");
            data.createCell(1).setCellValue("\u738b\u5c0f\u660e");
            data.createCell(2).setCellValue("xm@qq.com");
            data.createCell(3).setCellValue("13800138000");
            data.createCell(4).setCellValue("\u7537");
            data.createCell(5).setCellValue("\u524d\u7aef");
            data.createCell(6).setCellValue("\u7ecf\u7406");
            data.createCell(7).setCellValue("21st");
            data.createCell(8).setCellValue("20241003136");
            data.createCell(9).setCellValue("\u8f6f\u5de52402");
            data.createCell(10).setCellValue("\u662f");
            data.createCell(11).setCellValue("\u8f6f\u4ef6\u5de5\u7a0b");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            List<QtLabMemberImportRow> rows = QtLabMemberImportParser.parse(new ByteArrayInputStream(out.toByteArray()));
            assertEquals(1, rows.size());
            QtLabMemberImportRow row = rows.get(0);
            assertEquals("wangxiaoming", row.getUserName());
            assertEquals("FRONTEND", row.getMemberDepartment());
            assertEquals("1", row.getIsQuantaMember());
            assertEquals("MANAGER", row.getRoleCategory());
            assertEquals("0", row.getSex());
            assertTrue(row.getDeptId() == QtLabMemberImportParser.DEFAULT_DEPT_ID);
        }
    }
}
