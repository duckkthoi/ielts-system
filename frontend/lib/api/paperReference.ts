import axiosClient from '@/lib/axiosClient';
import { formatUrl } from '@/lib/api/base';
import type { ApiResponse } from '@/lib/types/api';
import type { BatchResult, CslItem, DoiLookup, ReferenceDto } from '@/lib/types/paper';

// ─── Thư viện trích dẫn của nhóm + tra DOI (docs/05 mục A.10, B24) ────────────

const URL = {
  list: '/api/papers/groups/{groupId}/references',
  batch: '/api/papers/groups/{groupId}/references/batch',
  one: '/api/papers/groups/{groupId}/references/{id}',
  doi: '/api/papers/doi'
} as const;

export async function list(groupId: number): Promise<ReferenceDto[]> {
  const res = (await axiosClient.get(formatUrl(URL.list, { groupId }), {
    controlLoading: true
  })) as unknown as ApiResponse<ReferenceDto[]>;
  return res.data;
}

/** DOI đã có trong thư viện → 409 DUPLICATE (axiosClient toast message của server). */
export async function add(groupId: number, csl: CslItem): Promise<ReferenceDto> {
  const res = (await axiosClient.post(formatUrl(URL.list, { groupId }), {
    csl
  })) as unknown as ApiResponse<ReferenceDto>;
  return res.data;
}

export async function addBatch(groupId: number, items: CslItem[]): Promise<BatchResult> {
  const res = (await axiosClient.post(formatUrl(URL.batch, { groupId }), {
    items
  })) as unknown as ApiResponse<BatchResult>;
  return res.data;
}

export async function update(groupId: number, id: number, csl: CslItem): Promise<ReferenceDto> {
  const res = (await axiosClient.put(formatUrl(URL.one, { groupId, id }), {
    csl
  })) as unknown as ApiResponse<ReferenceDto>;
  return res.data;
}

export async function remove(groupId: number, id: number): Promise<void> {
  await axiosClient.delete(formatUrl(URL.one, { groupId, id }));
}

/** Tra DOI (chưa lưu). Ô tra tự hiện trạng thái đang tải; lỗi (không có DOI, dịch vụ lỗi) vẫn toast message của server. */
export async function lookupDoi(value: string): Promise<DoiLookup> {
  const res = (await axiosClient.get(URL.doi, {
    params: { value },
    controlLoading: true
  })) as unknown as ApiResponse<DoiLookup>;
  return res.data;
}

export const paperReferenceApi = { list, add, addBatch, update, remove, lookupDoi };
