package com.ruoyi.qt.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.qt.domain.QtLabMemberImportRow;

/**
 * Parse a lab-member workbook and normalize dirty department / member flags.
 */
public final class QtLabMemberImportParser
{
    public static final long DEFAULT_DEPT_ID = 103L;

    private QtLabMemberImportParser()
    {
    }

    public static List<QtLabMemberImportRow> parse(InputStream inputStream)
    {
        try (Workbook workbook = WorkbookFactory.create(inputStream))
        {
            Sheet sheet = resolveSheet(workbook);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2)
            {
                throw new ServiceException("\u5bfc\u5165\u6210\u5458\u6570\u636e\u4e0d\u80fd\u4e3a\u7a7a");
            }
            DataFormatter formatter = new DataFormatter();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> columns = mapColumns(header, formatter);
            if (!columns.containsKey("userName"))
            {
                throw new ServiceException("\u7f3a\u5c11\u300c\u767b\u5f55\u540d\u79f0\u300d\u5217");
            }
            List<QtLabMemberImportRow> rows = new ArrayList<QtLabMemberImportRow>();
            for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++)
            {
                Row excelRow = sheet.getRow(i);
                if (excelRow == null)
                {
                    continue;
                }
                QtLabMemberImportRow row = readRow(excelRow, columns, formatter);
                if (row == null)
                {
                    continue;
                }
                row.setSourceRow(i + 1);
                rows.add(row);
            }
            if (rows.isEmpty())
            {
                throw new ServiceException("\u5bfc\u5165\u6210\u5458\u6570\u636e\u4e0d\u80fd\u4e3a\u7a7a");
            }
            return rows;
        }
        catch (ServiceException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new ServiceException("\u65e0\u6cd5\u89e3\u6790 Excel\uff1a" + ex.getMessage());
        }
    }

    public static String normalizeDepartment(String raw)
    {
        String value = StringUtils.trim(raw);
        if (StringUtils.isEmpty(value))
        {
            return value;
        }
        String upper = value.toUpperCase(Locale.ROOT);
        if ("PRODUCT".equals(upper) || "DESIGN".equals(upper)
                || "FRONTEND".equals(upper) || "BACKEND".equals(upper))
        {
            return upper;
        }
        if (value.contains("\u4ea7\u54c1") || "PRODUCT".equals(upper))
        {
            return "PRODUCT";
        }
        if (value.contains("\u8bbe\u8ba1") || "DESIGN".equals(upper))
        {
            return "DESIGN";
        }
        if (value.contains("\u524d\u7aef"))
        {
            return "FRONTEND";
        }
        if (value.contains("\u540e\u7aef"))
        {
            return "BACKEND";
        }
        return upper;
    }

    public static String normalizeMemberFlag(String raw)
    {
        String value = StringUtils.trim(raw);
        if (StringUtils.isEmpty(value))
        {
            return "1";
        }
        if ("0".equals(value) || "\u65b0\u751f".equals(value))
        {
            return "0";
        }
        return "1";
    }

    public static String normalizeSex(String raw)
    {
        String value = StringUtils.trim(raw);
        if ("\u7537".equals(value) || "0".equals(value))
        {
            return "0";
        }
        if ("\u5973".equals(value) || "1".equals(value))
        {
            return "1";
        }
        if (StringUtils.isEmpty(value))
        {
            return "2";
        }
        return "2";
    }

    public static String normalizeStatus(String raw)
    {
        String value = StringUtils.trim(raw);
        if ("1".equals(value) || "\u505c\u7528".equals(value))
        {
            return "1";
        }
        return "0";
    }

    public static String resolveRoleCategory(String title)
    {
        String value = StringUtils.trim(title);
        if (StringUtils.isEmpty(value))
        {
            return "MEMBER";
        }
        String compact = value.toLowerCase(Locale.ROOT).replace(" ", "");
        if (value.contains("\u8d1f\u8d23\u4eba") || compact.contains("vp") || compact.contains("\u526f\u603b\u88c1"))
        {
            return "MGMT";
        }
        if (value.contains("\u7ecf\u7406"))
        {
            return "MANAGER";
        }
        if (value.contains("\u5b9e\u4e60"))
        {
            return "INTERN";
        }
        return "MEMBER";
    }

    private static Sheet resolveSheet(Workbook workbook)
    {
        Sheet named = workbook.getSheet("\u6210\u5458\u540d\u5355");
        if (named != null)
        {
            return named;
        }
        return workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
    }

    private static Map<String, Integer> mapColumns(Row header, DataFormatter formatter)
    {
        Map<String, Integer> columns = new HashMap<String, Integer>();
        if (header == null)
        {
            return columns;
        }
        for (int i = 0; i < header.getLastCellNum(); i++)
        {
            String label = cellText(header.getCell(i), formatter);
            String key = headerKey(label);
            if (key != null && !columns.containsKey(key))
            {
                columns.put(key, i);
            }
        }
        return columns;
    }

    private static String headerKey(String label)
    {
        if (StringUtils.isEmpty(label))
        {
            return null;
        }
        if (label.contains("\u767b\u5f55\u540d\u79f0"))
        {
            return "userName";
        }
        if (label.contains("\u7528\u6237\u540d\u79f0") || "\u59d3\u540d".equals(label))
        {
            return "nickName";
        }
        if (label.contains("\u90ae\u7bb1"))
        {
            return "email";
        }
        if (label.contains("\u624b\u673a"))
        {
            return "phonenumber";
        }
        if (label.contains("\u6027\u522b"))
        {
            return "sex";
        }
        if (label.contains("\u8d26\u53f7\u72b6\u6001") || "\u72b6\u6001".equals(label))
        {
            return "status";
        }
        if (label.contains("\u6210\u5458\u7f16\u53f7"))
        {
            return "memberNo";
        }
        if (label.contains("\u6210\u5458\u90e8\u95e8"))
        {
            return "memberDepartment";
        }
        if (label.contains("\u804c\u79f0"))
        {
            return "memberTitle";
        }
        if (label.contains("\u5c4a\u6b21"))
        {
            return "memberCohort";
        }
        if (label.contains("\u5b66\u53f7"))
        {
            return "studentNo";
        }
        if (label.contains("\u73ed\u7ea7"))
        {
            return "className";
        }
        if (label.contains("\u4e13\u4e1a"))
        {
            return "major";
        }
        if (label.contains("\u662f\u5426\u5854\u5458") || label.contains("\u5854\u5458"))
        {
            return "isQuantaMember";
        }
        if (label.contains("\u90e8\u95e8\u7f16\u53f7"))
        {
            return "deptId";
        }
        return null;
    }

    private static QtLabMemberImportRow readRow(Row excelRow, Map<String, Integer> columns, DataFormatter formatter)
    {
        String userName = value(excelRow, columns, "userName", formatter);
        String nickName = value(excelRow, columns, "nickName", formatter);
        String email = value(excelRow, columns, "email", formatter);
        if (StringUtils.isEmpty(userName) && StringUtils.isEmpty(nickName) && StringUtils.isEmpty(email))
        {
            return null;
        }
        QtLabMemberImportRow row = new QtLabMemberImportRow();
        row.setUserName(userName);
        row.setNickName(nickName);
        row.setEmail(email);
        row.setPhonenumber(value(excelRow, columns, "phonenumber", formatter));
        row.setSex(normalizeSex(value(excelRow, columns, "sex", formatter)));
        row.setStatus(normalizeStatus(value(excelRow, columns, "status", formatter)));
        row.setMemberNo(value(excelRow, columns, "memberNo", formatter));
        row.setMemberDepartment(normalizeDepartment(value(excelRow, columns, "memberDepartment", formatter)));
        row.setMemberTitle(value(excelRow, columns, "memberTitle", formatter));
        row.setMemberCohort(value(excelRow, columns, "memberCohort", formatter));
        row.setStudentNo(value(excelRow, columns, "studentNo", formatter));
        row.setClassName(value(excelRow, columns, "className", formatter));
        row.setMajor(value(excelRow, columns, "major", formatter));
        row.setIsQuantaMember(normalizeMemberFlag(value(excelRow, columns, "isQuantaMember", formatter)));
        row.setRoleCategory(resolveRoleCategory(row.getMemberTitle()));
        String deptId = value(excelRow, columns, "deptId", formatter);
        row.setDeptId(StringUtils.isEmpty(deptId) ? DEFAULT_DEPT_ID : Long.valueOf(deptId.replace(".0", "")));
        return row;
    }

    private static String value(Row row, Map<String, Integer> columns, String key, DataFormatter formatter)
    {
        Integer index = columns.get(key);
        if (index == null)
        {
            return "";
        }
        return cellText(row.getCell(index), formatter);
    }

    private static String cellText(Cell cell, DataFormatter formatter)
    {
        if (cell == null)
        {
            return "";
        }
        return StringUtils.trim(formatter.formatCellValue(cell));
    }
}
