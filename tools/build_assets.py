"""Build immutable public metadata assets from the official KMA downloads.

Inputs and checksums are recorded in docs/ASSET_SOURCES.md. No service keys used.
"""
import argparse
import csv
import hashlib
import io
import json
import re
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
WN = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}

def xlsx(path):
    with zipfile.ZipFile(path) as z:
        strings = ["".join(e.itertext()) for e in ET.fromstring(z.read("xl/sharedStrings.xml")).findall("s:si", NS)]
        rows = []
        for row in ET.fromstring(z.read("xl/worksheets/sheet1.xml")).findall("s:sheetData/s:row", NS):
            values = {}
            for c in row.findall("s:c", NS):
                v = c.find("s:v", NS)
                value = v.text if v is not None else ""
                if c.get("t") == "s": value = strings[int(value)]
                values[re.sub(r"[0-9]", "", c.get("r"))] = value
            rows.append(values)
        return rows

def write_csv(path, fields, rows):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=fields, lineterminator="\n")
        w.writeheader(); w.writerows(rows)

def build(source, root):
    asset = root / "app/src/main/assets"
    grid = []
    for r in xlsx(source / "short-0.xlsx")[1:]:
        if r.get("A") != "kor" or not r.get("F") or not r.get("O"): continue
        grid.append(dict(code=r["B"], sido=r["C"], sigungu=r.get("D", ""), dong=r.get("E", ""),
                         nx=int(float(r["F"])), ny=int(float(r["G"])), lat=float(r["O"]), lon=float(r["N"])))
    mid = [dict(code=r["A"], name=r["B"], kind=r["C"]) for r in xlsx(source / "mid-1.xlsx")[1:]
           if r.get("A") and r.get("B") and r.get("C") in ("A", "C")]
    with zipfile.ZipFile(source / "asos.zip") as z:
        doc = ET.fromstring(z.read("word/document.xml"))
    ids = set()
    for table in doc.findall(".//w:tbl", WN):
        rows = [["".join(c.itertext()).strip() for c in r.findall("w:tc", WN)] for r in table.findall("w:tr", WN)]
        if rows and rows[0][:3] == ["지점", "지점명", "관리관서"]:
            for row in rows[1:]:
                for i in (0, 3):
                    if len(row) > i and row[i].isdigit(): ids.add(row[i])
    raw = (source / "kma-stations.csv").read_bytes().decode("cp949").lstrip()
    current = {}
    for r in csv.DictReader(io.StringIO(raw)):
        if r.get("지점") not in ids or r.get("종료일") or not r.get("위도") or not r.get("경도"): continue
        old = current.get(r["지점"])
        if old is None or r["시작일"] > old["시작일"]: current[r["지점"]] = r
    stations = [dict(stationId=k, name=r["지점명"], lat=r["위도"], lon=r["경도"]) for k,r in sorted(current.items(),key=lambda p:int(p[0]))]
    assert len(grid) > 3000 and len(stations) >= 90, "Unexpected official input completeness"
    write_csv(asset / "region_grid.csv", ["code","sido","sigungu","dong","nx","ny","lat","lon"], grid)
    write_csv(asset / "asos_stations.csv", ["stationId","name","lat","lon"], stations)
    write_csv(asset / "mid_regions.csv", ["code","name","kind"], mid)
    samples = []
    for term in ["서울특별시", "부산광역시", "대구광역시", "광산구", "대전광역시", "강릉시", "제주시", "울릉읍", "백령면", "서귀포시"]:
        matches = [r for r in grid if any(term in r[k] for k in ("sido","sigungu","dong"))]
        r = min(matches,key=lambda r:len(r["sido"]+r["sigungu"]+r["dong"]))
        samples.append({k:r[k] for k in ["code","sido","sigungu","dong","lat","lon","nx","ny"]})
    write_csv(root / "app/src/test/resources/fixtures/grid_samples.csv",["code","sido","sigungu","dong","lat","lon","nx","ny"],samples)
    inputs = ["short-0.xlsx","mid-1.xlsx","asos.zip","kma-stations.csv"]
    hashes = {name:hashlib.sha256((source/name).read_bytes()).hexdigest() for name in inputs}
    report = {"region_count":len(grid),"asos_count":len(stations),"mid_count":len(mid),"inputs_sha256":hashes}
    (source / "asset-build.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    print(json.dumps(report))

if __name__ == "__main__":
    parser=argparse.ArgumentParser();parser.add_argument("--source-dir",type=Path,required=True)
    args=parser.parse_args();build(args.source_dir,Path(__file__).resolve().parents[1])
