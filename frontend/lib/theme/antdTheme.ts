import type { ThemeConfig } from 'antd';

import { BRAND, FONT_FAMILY, LAYOUT, NEUTRAL, RADIUS, SEMANTIC, SHADOW } from './tokens';

// ─── Theme Ant Design ─────────────────────────────────────────────────────────
// Ánh xạ design token (docs/07-design-system.md) sang token của antd.

export const antdTheme: ThemeConfig = {
  cssVar: { key: 'ielts' },
  token: {
    colorPrimary: BRAND[600],
    colorLink: BRAND[600],
    colorSuccess: SEMANTIC.success.text,
    colorWarning: SEMANTIC.warning.text,
    colorError: SEMANTIC.error.text,
    colorInfo: SEMANTIC.info.text,

    colorTextBase: NEUTRAL[900],
    colorText: NEUTRAL[700],
    colorTextHeading: NEUTRAL[900],
    colorTextSecondary: NEUTRAL[500],
    colorTextPlaceholder: NEUTRAL[400],
    colorBorder: NEUTRAL[300],
    colorBorderSecondary: NEUTRAL[100],
    colorBgLayout: NEUTRAL[25],
    colorBgContainer: NEUTRAL[0],
    colorFillAlter: NEUTRAL[50],

    fontFamily: FONT_FAMILY.sans,
    fontFamilyCode: FONT_FAMILY.mono,
    fontSize: 14,

    borderRadius: RADIUS.md,
    borderRadiusSM: RADIUS.sm,
    borderRadiusLG: RADIUS.lg,
    controlHeight: 36,
    controlHeightSM: 32,
    controlHeightLG: 40,

    boxShadow: SHADOW.md,
    boxShadowSecondary: SHADOW.lg,

    motionDurationFast: '0.12s',
    motionDurationMid: '0.2s',
    motionDurationSlow: '0.3s'
  },
  components: {
    Layout: {
      headerBg: NEUTRAL[0],
      headerHeight: LAYOUT.headerHeight,
      headerPadding: '0 24px',
      siderBg: NEUTRAL[0],
      bodyBg: NEUTRAL[25]
    },
    Menu: {
      itemBg: NEUTRAL[0],
      itemSelectedBg: BRAND[50],
      itemSelectedColor: BRAND[700],
      itemHoverBg: NEUTRAL[25],
      itemBorderRadius: RADIUS.md
    },
    Table: {
      headerBg: NEUTRAL[50],
      rowHoverBg: NEUTRAL[25]
    },
    Card: {
      borderRadiusLG: RADIUS.lg
    }
  }
};
