# Design QA

- Source visual truth: `D:\download\宿舍管理系统\8.6\样式预览.html` and its local styles.
- Implementation: `http://127.0.0.1:8088/visitor/dormitory`.
- Primary comparison viewport: 2048 × 953; additional checks: 1366 desktop and 390 mobile.
- Captures: `qa-output/dormitory/0-8.6参考页面.png` through `qa-output/dormitory/11-统计总览-手机.png`.

## Coverage

- Compared statistics, floor plan, warning board, stay ledger, utilities, history, and settings.
- Verified group/site/building selectors and booking/resource dialogs.
- Checked desktop/mobile overflow and collected browser console/page errors.

## Result

- The complete dormitory module now uses the 8.6 visual system: compact white header, two-row region pills, white navigation, pale-blue active states, blue primary actions, bordered cards, compact tables, room/bed state colors, forms, dialogs, and responsive navigation.
- Browser errors: none. Desktop overflow: none. Mobile document overflow: none; wide tables scroll inside their own containers.

Final result: passed
