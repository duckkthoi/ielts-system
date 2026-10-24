import type { TAB } from './const';

export type NotificationTab = (typeof TAB)[keyof typeof TAB];
