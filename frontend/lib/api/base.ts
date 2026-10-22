import type { AxiosRequestConfig } from 'axios';

import axiosClient from '@/lib/axiosClient';
import type { ApiResponse } from '@/lib/types/api';

/**
 * Thay biến đường dẫn trong template, vd: formatUrl('/api/teacher/classes/{id}', { id: 5 }).
 */
export function formatUrl(template: string, pathParams?: Record<string, string | number>): string {
  let url = template;
  if (pathParams) {
    for (const [key, val] of Object.entries(pathParams)) {
      url = url.replace(new RegExp(`\\{${key}\\}`, 'g'), encodeURIComponent(String(val)));
    }
  }
  return url;
}

/** Các hàm CRUD REST chuẩn cho một resource. */
export interface BaseCrudService<T, C = Partial<T>, U = Partial<T>> {
  list: (params?: Record<string, unknown>, config?: AxiosRequestConfig) => Promise<ApiResponse<T[]>>;
  getDetail: (id: number, config?: AxiosRequestConfig) => Promise<ApiResponse<T>>;
  create: (data: C, config?: AxiosRequestConfig) => Promise<ApiResponse<T>>;
  update: (id: number, data: U, config?: AxiosRequestConfig) => Promise<ApiResponse<T>>;
  remove: (id: number, config?: AxiosRequestConfig) => Promise<ApiResponse<null>>;
}

/**
 * Tạo bộ CRUD cho resource REST, vd: createBaseCrud<Classroom>('/api/teacher/classes').
 * Hàm custom ngoài CRUD viết riêng trong file domain rồi gộp vào object export.
 */
export function createBaseCrud<T, C = Partial<T>, U = Partial<T>>(resourcePath: string): BaseCrudService<T, C, U> {
  return {
    list: async (params, config) =>
      (await axiosClient.get(resourcePath, { params, ...config })) as unknown as ApiResponse<T[]>,
    getDetail: async (id, config) =>
      (await axiosClient.get(`${resourcePath}/${id}`, config)) as unknown as ApiResponse<T>,
    create: async (data, config) => (await axiosClient.post(resourcePath, data, config)) as unknown as ApiResponse<T>,
    update: async (id, data, config) =>
      (await axiosClient.put(`${resourcePath}/${id}`, data, config)) as unknown as ApiResponse<T>,
    remove: async (id, config) =>
      (await axiosClient.delete(`${resourcePath}/${id}`, config)) as unknown as ApiResponse<null>
  };
}
