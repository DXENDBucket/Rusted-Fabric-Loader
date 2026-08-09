#!/usr/bin/env python3
"""Generate the bilingual INI Essentials reference workbook from the tracked CSV catalog."""

from __future__ import annotations

import csv
from pathlib import Path

from openpyxl import Workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "src/main/resources/ini_essentials/fields.csv"
EVENT_DATA_SOURCE = ROOT / "src/main/resources/ini_essentials/event-data.csv"
OUTPUT = Path(__file__).with_name("INI Essentials Unit Modding Reference.xlsx")

HEADERS = {
    "English": [
        "Version Added", "Code", "Section Type", "Extension Type", "Value Type",
        "Default", "Multiplayer Impact", "Description and usage", "Example",
    ],
    "简体中文": [
        "加入版本", "代码", "节类型", "扩展类型", "值类型",
        "默认值", "联机影响", "说明与用法", "示例",
    ],
}


def load_rows(source: Path) -> list[dict[str, str]]:
    with source.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def add_reference_sheet(workbook: Workbook, title: str, rows: list[dict[str, str]]) -> None:
    sheet = workbook.create_sheet(title)
    headers = HEADERS[title]
    sheet.append(headers)
    description = "description_en" if title == "English" else "description_zh"
    for row in rows:
        sheet.append([
            row["version_added"], row["code"], row["section"], row["extension_type"],
            row["value_type"], row["default"], row["multiplayer_impact"],
            row[description], row["example"],
        ])

    dark = PatternFill("solid", fgColor="4F4F4F")
    pale = PatternFill("solid", fgColor="E7E6E6")
    edge = Side(style="thin", color="A6A6A6")
    for cell in sheet[1]:
        cell.fill = dark
        cell.font = Font(color="FFFFFF", bold=True)
        cell.alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)
        cell.border = Border(bottom=edge)
    for row_index in range(2, sheet.max_row + 1):
        if row_index % 2 == 0:
            for cell in sheet[row_index]:
                cell.fill = pale
        for cell in sheet[row_index]:
            cell.alignment = Alignment(vertical="top", wrap_text=True)
    widths = [15, 24, 18, 20, 14, 12, 22, 72, 30]
    for index, width in enumerate(widths, 1):
        sheet.column_dimensions[get_column_letter(index)].width = width
    sheet.row_dimensions[1].height = 32
    sheet.freeze_panes = "A2"
    sheet.auto_filter.ref = sheet.dimensions


def add_event_data_sheet(workbook: Workbook, title: str,
                         rows: list[dict[str, str]]) -> None:
    sheet = workbook.create_sheet(title)
    chinese = title.endswith("中文")
    sheet.append([
        "加入版本" if chinese else "Version Added",
        "事件" if chinese else "Event",
        "eventData 名称" if chinese else "eventData Name",
        "值类型" if chinese else "Value Type",
        "联机影响" if chinese else "Multiplayer Impact",
        "说明与用法" if chinese else "Description and usage",
        "示例" if chinese else "Example",
    ])
    description = "description_zh" if chinese else "description_en"
    for row in rows:
        sheet.append([
            row["version_added"], row["event"], row["event_data_name"],
            row["value_type"], row["multiplayer_impact"],
            row[description], row["example"],
        ])
    dark = PatternFill("solid", fgColor="4F4F4F")
    pale = PatternFill("solid", fgColor="E7E6E6")
    edge = Side(style="thin", color="A6A6A6")
    for cell in sheet[1]:
        cell.fill = dark
        cell.font = Font(color="FFFFFF", bold=True)
        cell.alignment = Alignment(horizontal="center", vertical="center", wrap_text=True)
        cell.border = Border(bottom=edge)
    for row_index in range(2, sheet.max_row + 1):
        for cell in sheet[row_index]:
            if row_index % 2 == 0:
                cell.fill = pale
            cell.alignment = Alignment(vertical="top", wrap_text=True)
    for index, width in enumerate([15, 18, 24, 14, 22, 72, 48], 1):
        sheet.column_dimensions[get_column_letter(index)].width = width
    sheet.row_dimensions[1].height = 32
    sheet.freeze_panes = "A2"
    sheet.auto_filter.ref = sheet.dimensions


def add_about_sheet(workbook: Workbook) -> None:
    sheet = workbook.active
    sheet.title = "About 关于"
    lines = [
        ("INI Essentials Unit Modding Reference", "title"),
        ("Generated from the tracked fields.csv and event-data.csv catalogs. Do not edit this workbook as the source.", "text"),
        ("本表由仓库内的 fields.csv 和 event-data.csv 自动生成，请勿把此工作簿作为源文件直接修改。", "text"),
        ("", "text"),
        ("Compatibility rule / 兼容原则", "heading"),
        ("Native keys with native-valid values stay on the native parser path.", "text"),
        ("原版字段使用原版合法值时，始终保留原版解析路径。", "text"),
        ("Extensions activate only for a documented new key, value range, or format.", "text"),
        ("只有代码表明确记录的新字段、新取值范围或新格式才会激活扩展。", "text"),
        ("Omitted optional fields keep their documented defaults and do not alter native behavior.", "text"),
        ("不填写可选字段时使用表中默认值，且不改变原版行为。", "text"),
    ]
    for row, (value, style) in enumerate(lines, 1):
        cell = sheet.cell(row, 1, value)
        cell.alignment = Alignment(wrap_text=True, vertical="top")
        if style == "title":
            cell.font = Font(size=18, bold=True, color="FFFFFF")
            cell.fill = PatternFill("solid", fgColor="4F4F4F")
            sheet.row_dimensions[row].height = 30
        elif style == "heading":
            cell.font = Font(size=12, bold=True)
    sheet.column_dimensions["A"].width = 105


def main() -> None:
    rows = load_rows(SOURCE)
    event_data_rows = load_rows(EVENT_DATA_SOURCE)
    workbook = Workbook()
    add_about_sheet(workbook)
    add_reference_sheet(workbook, "English", rows)
    add_reference_sheet(workbook, "简体中文", rows)
    add_event_data_sheet(workbook, "Event Data English", event_data_rows)
    add_event_data_sheet(workbook, "Event Data 中文", event_data_rows)
    workbook.save(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    main()
