#!/usr/bin/env python3
"""Generate the bilingual INI Essentials reference workbook from tracked CSV catalogs."""

from __future__ import annotations

import argparse
import csv
from collections import OrderedDict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from openpyxl import Workbook, load_workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.hyperlink import Hyperlink


ROOT = Path(__file__).resolve().parents[1]
FIELD_SOURCE = ROOT / "src/main/resources/ini_essentials/fields.csv"
EVENT_DATA_SOURCE = ROOT / "src/main/resources/ini_essentials/event-data.csv"
OUTPUT = Path(__file__).with_name("INI Essentials Unit Modding Reference.xlsx")

FONT_NAME = "Arial"
TITLE_COLOR = "EFEFEF"
NOTICE_COLOR = "00FF00"
INDEX_COLOR = "D9D9D9"
GRID_COLOR = "A6A6A6"
ROW_ALT_COLOR = "F3F3F3"
WHITE = "FFFFFF"
BLACK = "000000"

# Colors copied from the corresponding section bands in the original 1.15/1.16 reference.
SECTION_PALETTES = {
    "core": ("0F9D58", "0D904F"),
    "canbuild": ("8E7CC3", "674EA7"),
    "graphics": ("EA9999", "CC0000"),
    "attack": ("6D9EEB", "3C78D8"),
    "turret": ("8E7CC3", "674EA7"),
    "projectile": ("BF9000", "7F6000"),
    "movement": ("D5A6BD", "A64D79"),
    "ai": ("DD7E6B", "A61C00"),
    "leg_arm": ("134F5C", "0C343D"),
    "attachment": ("CC4125", "85200C"),
    "action": ("FF9900", "B45F06"),
    "effect": ("A64D79", "741B47"),
    "animation": ("45818E", "134F5C"),
    "resource": ("134F5C", "0C343D"),
    "comment_template": ("134F5C", "0C343D"),
    "other": ("4A86E8", "1155CC"),
}

COLUMN_WIDTHS = [15, 28, 18, 68, 42, 22, 16, 23]


@dataclass(frozen=True)
class ReferenceGroup:
    key: str
    section_en: str
    section_zh: str
    summary_en: str
    summary_zh: str
    palette: str
    rows: tuple[dict[str, str], ...]
    event_data: bool = False


def load_rows(source: Path) -> list[dict[str, str]]:
    with source.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def section_key(section: str) -> str:
    value = section.lower()
    if "core" in value:
        return "core"
    if "action" in value:
        return "action"
    if "graphics" in value:
        return "graphics"
    if "attack" in value:
        return "attack"
    if "turret" in value:
        return "turret"
    if "projectile" in value:
        return "projectile"
    if "movement" in value:
        return "movement"
    if "attachment" in value:
        return "attachment"
    if "effect" in value:
        return "effect"
    if "animation" in value:
        return "animation"
    if "resource" in value:
        return "resource"
    if "canbuild" in value:
        return "canbuild"
    if "leg" in value or "arm" in value:
        return "leg_arm"
    if "comment" in value or "template" in value:
        return "comment_template"
    if "ai" in value:
        return "ai"
    return "other"


def make_groups(field_rows: list[dict[str, str]],
                event_rows: list[dict[str, str]]) -> list[ReferenceGroup]:
    grouped: OrderedDict[str, list[dict[str, str]]] = OrderedDict()
    section_names: dict[str, str] = {}
    for row in field_rows:
        key = section_key(row["section"])
        grouped.setdefault(key, []).append(row)
        section_names.setdefault(key, row["section"])

    groups: list[ReferenceGroup] = []
    for key, rows in grouped.items():
        if key == "core":
            section_en, section_zh = "[core]", "[core]"
            summary_en = "Core unit functions and opt-in static unit fields"
            summary_zh = "单位核心功能与可选的静态单位字段"
        elif key == "action":
            section_en = "[action_NAME] / [hiddenAction_NAME]"
            section_zh = "[action_NAME] / [hiddenAction_NAME]"
            summary_en = "Action effects that run through the native custom-action chain"
            summary_zh = "通过原版自定义动作链执行的动作效果"
        else:
            section_en = section_names[key]
            section_zh = section_names[key]
            summary_en = "INI Essentials additions for this native section"
            summary_zh = "该原版节对应的 INI Essentials 扩展"
        groups.append(ReferenceGroup(
            key, section_en, section_zh, summary_en, summary_zh,
            key, tuple(rows), False))

    if event_rows:
        groups.append(ReferenceGroup(
            "took_damage_event_data",
            "tookDamage — eventData(...)",
            "tookDamage — eventData(...) 事件数据",
            "Extra context values for the native tookDamage action event",
            "为原版 tookDamage 动作事件补充的上下文值",
            "action", tuple(event_rows), True))
    return groups


def fill(color: str) -> PatternFill:
    return PatternFill("solid", fgColor=argb(color))


def argb(color: str) -> str:
    return color if len(color) == 8 else "FF" + color


def internal_link(cell, sheet_title: str, target: str) -> None:
    cell.hyperlink = Hyperlink(
        ref=cell.coordinate,
        location=f"'{sheet_title}'!{target}",
        display=str(cell.value) if cell.value is not None else None,
    )


def style_range(sheet, row: int, start: int, end: int, *,
                background: str, font_color: str = BLACK, bold: bool = False,
                horizontal: str = "left", size: float = 10.0) -> None:
    edge = Side(style="thin", color=argb(GRID_COLOR))
    for column in range(start, end + 1):
        cell = sheet.cell(row, column)
        cell.fill = fill(background)
        cell.font = Font(
            name=FONT_NAME, size=size, bold=bold, color=argb(font_color))
        cell.alignment = Alignment(
            horizontal=horizontal, vertical="center", wrap_text=True)
        cell.border = Border(left=edge, right=edge, top=edge, bottom=edge)


def shortcut_rows(group_count: int) -> int:
    return max(1, (group_count + 2) // 3)


def add_shortcuts(sheet, groups: list[ReferenceGroup], starts: dict[str, int],
                  start_row: int, chinese: bool) -> int:
    spans = ((1, 2), (3, 5), (6, 8))
    for index, group in enumerate(groups):
        row = start_row + index // 3
        first, last = spans[index % 3]
        sheet.merge_cells(start_row=row, start_column=first,
                          end_row=row, end_column=last)
        cell = sheet.cell(row, first)
        cell.value = group.section_zh if chinese else group.section_en
        internal_link(cell, sheet.title, f"A{starts[group.key]}")
        section_color, _ = SECTION_PALETTES.get(
            group.palette, SECTION_PALETTES["other"])
        style_range(sheet, row, first, last,
                    background=section_color, font_color=WHITE,
                    bold=True, horizontal="center", size=10.0)
        cell.font = Font(
            name=FONT_NAME, size=10.0, bold=True,
            underline="single", color=argb(WHITE))
        sheet.row_dimensions[row].height = 27
    return start_row + shortcut_rows(len(groups))


def add_group(sheet, group: ReferenceGroup, start_row: int,
              chinese: bool, shortcut_target: str) -> int:
    section_color, header_color = SECTION_PALETTES.get(
        group.palette, SECTION_PALETTES["other"])
    section_name = group.section_zh if chinese else group.section_en
    summary = group.summary_zh if chinese else group.summary_en

    style_range(sheet, start_row, 1, 8,
                background=section_color, font_color=WHITE, size=10.0)
    sheet.cell(start_row, 1, "Version" if not chinese else "版本")
    sheet.cell(start_row, 2, "Section" if not chinese else "节")
    sheet.cell(start_row, 4, section_name)
    sheet.cell(start_row, 5, summary)
    sheet.merge_cells(start_row=start_row, start_column=5,
                      end_row=start_row, end_column=7)
    back = sheet.cell(start_row, 8, "↑ Shortcuts" if not chinese else "↑ 返回节导航")
    internal_link(back, sheet.title, shortcut_target)
    back.font = Font(
        name=FONT_NAME, size=10.0, bold=True,
        underline="single", color=argb(WHITE))
    sheet.cell(start_row, 4).font = Font(
        name=FONT_NAME, size=10.0, bold=True, color=argb(WHITE))
    sheet.row_dimensions[start_row].height = 34

    header_row = start_row + 1
    headers = (
        ["加入版本", "代码", "值类型", "说明与用法", "实际用法与示例",
         "扩展类型", "默认值", "联机影响"]
        if chinese else
        ["Version Added", "Code", "Value Type", "Description and usage",
         "Actual usage with examples", "Extension Type", "Default",
         "Multiplayer Impact"]
    )
    style_range(sheet, header_row, 1, 8,
                background=header_color, font_color=WHITE,
                bold=True, horizontal="center", size=11.0)
    for column, value in enumerate(headers, 1):
        sheet.cell(header_row, column, value)
    sheet.row_dimensions[header_row].height = 31

    description_key = "description_zh" if chinese else "description_en"
    for offset, row in enumerate(group.rows, 2):
        target_row = start_row + offset
        if group.event_data:
            values = [
                row["version_added"], row["event_data_name"], row["value_type"],
                row[description_key], row["example"],
                "native_event_data", "available during event", row["multiplayer_impact"],
            ]
        else:
            values = [
                row["version_added"], row["code"], row["value_type"],
                row[description_key], row["example"], row["extension_type"],
                row["default"], row["multiplayer_impact"],
            ]
        background = WHITE if offset % 2 == 0 else ROW_ALT_COLOR
        style_range(sheet, target_row, 1, 8, background=background)
        for column, value in enumerate(values, 1):
            cell = sheet.cell(target_row, column, value)
            cell.alignment = Alignment(
                horizontal="center" if column in (1, 3, 6, 7, 8) else "left",
                vertical="top", wrap_text=True)
            if column == 2:
                cell.font = Font(
                    name=FONT_NAME, size=10.0, bold=True, color=argb(BLACK))
        length = max(len(str(values[3])), len(str(values[4])))
        sheet.row_dimensions[target_row].height = max(28, min(88, 19 + length // 55 * 13))

    return start_row + len(group.rows) + 3


def add_reference_sheet(workbook: Workbook, title: str,
                        groups: list[ReferenceGroup], chinese: bool) -> None:
    sheet = workbook.create_sheet(title)
    sheet.sheet_view.showGridLines = False

    sheet.merge_cells("A1:H1")
    sheet["A1"] = ("INI Essentials Unit Modding Reference"
                   if not chinese else "INI Essentials 单位模组代码表")
    style_range(sheet, 1, 1, 8,
                background=TITLE_COLOR, bold=True, horizontal="center", size=14.0)
    sheet.row_dimensions[1].height = 38

    sheet.merge_cells("A2:H2")
    sheet["A2"] = (
        "Generated from tracked CSV catalogs. Section colors follow the original Rusted Warfare reference."
        if not chinese else
        "由仓库内 CSV 代码表生成；各节颜色沿用原版 Rusted Warfare 代码表。"
    )
    style_range(sheet, 2, 1, 8,
                background=NOTICE_COLOR, bold=True, horizontal="center", size=10.0)
    sheet.row_dimensions[2].height = 29

    sheet.merge_cells("A3:H3")
    sheet["A3"] = "Section shortcuts — click to jump" if not chinese else "节快捷导航——点击即可跳转"
    style_range(sheet, 3, 1, 8,
                background=INDEX_COLOR, bold=True, horizontal="center", size=11.0)
    sheet.row_dimensions[3].height = 28

    index_rows = shortcut_rows(len(groups))
    first_group_row = 4 + index_rows + 1
    starts: dict[str, int] = {}
    cursor = first_group_row
    for group in groups:
        starts[group.key] = cursor
        cursor += len(group.rows) + 3

    add_shortcuts(sheet, groups, starts, 4, chinese)

    cursor = first_group_row
    for group in groups:
        cursor = add_group(sheet, group, cursor, chinese, "A3")

    for index, width in enumerate(COLUMN_WIDTHS, 1):
        sheet.column_dimensions[get_column_letter(index)].width = width
    sheet.freeze_panes = f"A{first_group_row}"
    sheet.auto_filter.ref = None
    sheet.page_setup.orientation = "landscape"
    sheet.page_setup.fitToWidth = 1
    sheet.sheet_properties.pageSetUpPr.fitToPage = True
    sheet.print_options.horizontalCentered = True


def add_about_sheet(workbook: Workbook) -> None:
    sheet = workbook.active
    sheet.title = "About 关于"
    sheet.sheet_view.showGridLines = False
    sheet.merge_cells("A1:H1")
    sheet["A1"] = "INI Essentials Unit Modding Reference"
    style_range(sheet, 1, 1, 8,
                background=TITLE_COLOR, bold=True, horizontal="center", size=16.0)
    sheet.row_dimensions[1].height = 42

    lines = [
        "Generated from fields.csv and event-data.csv. Do not edit this workbook as the source.",
        "本表由 fields.csv 和 event-data.csv 自动生成，请勿把工作簿作为源文件直接修改。",
        "Native keys with native-valid values stay on the native parser path.",
        "原版字段使用原版合法值时，始终保留原版解析路径。",
        "Extensions activate only for a documented new key, value range, format, or event-data name.",
        "只有代码表明确记录的新字段、新取值范围、新格式或事件数据名才会激活扩展。",
    ]
    for row, value in enumerate(lines, 3):
        sheet.merge_cells(start_row=row, start_column=1, end_row=row, end_column=8)
        sheet.cell(row, 1, value)
        style_range(sheet, row, 1, 8,
                    background=WHITE if row % 2 else ROW_ALT_COLOR, size=11.0)
        sheet.row_dimensions[row].height = 27

    for row, (label, target, color) in enumerate((
            ("Open English reference / 打开英文代码表", "English", "4A86E8"),
            ("打开简体中文代码表 / Open Chinese reference", "简体中文", "0F9D58")), 10):
        sheet.merge_cells(start_row=row, start_column=2, end_row=row, end_column=7)
        cell = sheet.cell(row, 2, label)
        internal_link(cell, target, "A1")
        style_range(sheet, row, 2, 7,
                    background=color, font_color=WHITE,
                    bold=True, horizontal="center", size=11.0)
        cell.font = Font(
            name=FONT_NAME, size=11.0, bold=True,
            underline="single", color=argb(WHITE))
        sheet.row_dimensions[row].height = 31

    for index, width in enumerate(COLUMN_WIDTHS, 1):
        sheet.column_dimensions[get_column_letter(index)].width = width


def build_workbook() -> Workbook:
    groups = make_groups(load_rows(FIELD_SOURCE), load_rows(EVENT_DATA_SOURCE))
    workbook = Workbook()
    workbook.properties.title = "INI Essentials Unit Modding Reference"
    workbook.properties.creator = "Rusted Fabric Loader / INI Essentials"
    workbook.properties.description = "Generated bilingual reference for INI Essentials"
    add_about_sheet(workbook)
    add_reference_sheet(workbook, "English", groups, False)
    add_reference_sheet(workbook, "简体中文", groups, True)
    return workbook


def workbook_signature(workbook: Workbook) -> tuple:
    result = []
    for sheet in workbook.worksheets:
        cells = []
        for row in sheet.iter_rows():
            for cell in row:
                if cell.value is None and cell.hyperlink is None:
                    continue
                color = cell.fill.fgColor
                fill_value = color.rgb if color.type == "rgb" else str(color.indexed)
                location = cell.hyperlink.location if cell.hyperlink else None
                cells.append((cell.coordinate, cell.value, fill_value,
                              cell.font.bold, cell.font.color.rgb
                              if cell.font.color and cell.font.color.type == "rgb" else None,
                              location))
        result.append((sheet.title, sheet.freeze_panes,
                       tuple(sorted(str(value) for value in sheet.merged_cells.ranges)),
                       tuple(cells)))
    return tuple(result)


def check_output(expected: Workbook) -> None:
    if not OUTPUT.exists():
        raise SystemExit(f"Missing generated workbook: {OUTPUT}")
    actual = load_workbook(OUTPUT, read_only=False, data_only=False)
    if workbook_signature(expected) != workbook_signature(actual):
        raise SystemExit(
            "Generated workbook is stale; run docs/generate_reference.py and commit the result")
    print(f"Reference workbook is current: {OUTPUT}")


def main(argv: Iterable[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check", action="store_true",
        help="verify that the committed workbook matches the CSV catalogs and generator",
    )
    args = parser.parse_args(argv)
    workbook = build_workbook()
    if args.check:
        check_output(workbook)
        return
    workbook.save(OUTPUT)
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    main()
