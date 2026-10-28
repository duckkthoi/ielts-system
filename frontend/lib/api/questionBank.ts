import axiosClient from '@/lib/axiosClient';
import type { ApiResponse } from '@/lib/types/api';
import type {
  QuestionBankDetail,
  QuestionBankListParams,
  QuestionBankRequest,
  QuestionBankSummary
} from '@/lib/types/questionBank';

const URL = '/api/teacher/question-bank';

export async function list(params: QuestionBankListParams): Promise<ApiResponse<QuestionBankSummary[]>> {
  return (await axiosClient.get(URL, {
    params,
    controlLoading: false
  })) as unknown as ApiResponse<QuestionBankSummary[]>;
}

export async function detail(id: number): Promise<QuestionBankDetail> {
  const response = (await axiosClient.get(`${URL}/${id}`, {
    controlLoading: false
  })) as unknown as ApiResponse<QuestionBankDetail>;
  return response.data;
}

export async function create(body: QuestionBankRequest): Promise<QuestionBankDetail> {
  const response = (await axiosClient.post(URL, body, {
    controlLoading: false,
    toastError: false
  })) as unknown as ApiResponse<QuestionBankDetail>;
  return response.data;
}

export async function replace(id: number, body: QuestionBankRequest): Promise<QuestionBankDetail> {
  const response = (await axiosClient.put(`${URL}/${id}`, body, {
    controlLoading: false,
    toastError: false
  })) as unknown as ApiResponse<QuestionBankDetail>;
  return response.data;
}

export async function remove(id: number): Promise<void> {
  await axiosClient.delete(`${URL}/${id}`, { controlLoading: false });
}

export const questionBankApi = { list, detail, create, replace, remove };
