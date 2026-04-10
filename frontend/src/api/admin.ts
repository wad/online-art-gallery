import { api } from './client'
import type {
  PageResponse, ImageResponse, TagResponse, GalleryResponse,
  TourResponse, ImageUsageResponse, AsyncJobResponse, ViewStatsResponse, ThemeOption,
} from './types'

// --- Auth ---
export const login = (password: string) =>
  api.post<{ token: string }>('/auth/login', { password })

export const changePassword = (currentPassword: string, newPassword: string) =>
  api.put('/admin/password', { currentPassword, newPassword })

// --- Images ---
export const listImages = (params: {
  q?: string; tag?: string; nsfw?: boolean; orphan?: boolean
  sort?: string; dir?: string; page?: number; size?: number
}) => api.get<PageResponse<ImageResponse>>('/admin/images', { params })

export const getImage = (publicId: string) =>
  api.get<ImageResponse>(`/admin/images/${publicId}`)

export const createImage = (data: object) =>
  api.post<ImageResponse>('/admin/images', data)

export const updateImage = (publicId: string, data: object) =>
  api.put(`/admin/images/${publicId}`, data)

export const deleteImage = (publicId: string, substituteShortId: string) =>
  api.delete(`/admin/images/${publicId}`, { params: { substituteShortId } })

export const getImageUsage = (publicId: string) =>
  api.get<ImageUsageResponse>(`/admin/images/${publicId}/usage`)

export const bulkLoadImages = (data: { urls: string[]; galleryPublicId: string }) =>
  api.post('/admin/images/bulk', data)

export const bulkUpdateMetadata = (data: object) =>
  api.put('/admin/images/bulk-metadata', data)

export const regenerateThumbnail = (publicId: string) =>
  api.post(`/admin/images/${publicId}/regenerate-thumbnail`)

export const regenerateAllThumbnails = () =>
  api.post<{ jobId: string }>('/admin/images/regenerate-all-thumbnails')

export const getJob = (jobId: string) =>
  api.get<AsyncJobResponse>(`/admin/images/jobs/${jobId}`)

export const addTagToImage = (publicId: string, tagPublicId: string) =>
  api.post(`/admin/images/${publicId}/tags`, { tagPublicId })

export const removeTagFromImage = (publicId: string, tagPublicId: string) =>
  api.delete(`/admin/images/${publicId}/tags/${tagPublicId}`)

// --- Tags ---
export const listTags = () =>
  api.get<TagResponse[]>('/admin/tags')

export const createTag = (name: string) =>
  api.post<TagResponse>('/admin/tags', { name })

export const deleteTag = (publicId: string) =>
  api.delete(`/admin/tags/${publicId}`)

// --- Galleries ---
export const listGalleries = () =>
  api.get<GalleryResponse[]>('/admin/galleries')

export const getGallery = (publicId: string) =>
  api.get<GalleryResponse>(`/admin/galleries/${publicId}`)

export const createGallery = (data: object) =>
  api.post<GalleryResponse>('/admin/galleries', data)

export const updateGallery = (publicId: string, data: object) =>
  api.put<GalleryResponse>(`/admin/galleries/${publicId}`, data)

export const deleteGallery = (publicId: string) =>
  api.delete(`/admin/galleries/${publicId}`)

export const setDefaultGallery = (publicId: string) =>
  api.put(`/admin/galleries/${publicId}/default`)

export const reorderGalleries = (items: Array<{ publicId: string; sortOrder: number }>) =>
  api.put('/admin/galleries/reorder', items)

export const addImagesToGallery = (publicId: string, imagePublicIds: string[]) =>
  api.post(`/admin/galleries/${publicId}/images`, { imagePublicIds })

export const removeImageFromGallery = (galleryPublicId: string, imagePublicId: string) =>
  api.delete(`/admin/galleries/${galleryPublicId}/images/${imagePublicId}`)

export const exportGallery = (publicId: string) =>
  api.get(`/admin/galleries/${publicId}/export`)

export const importGallery = (data: object) =>
  api.post('/admin/galleries/import', data)

// --- Tours ---
export const listTours = (galleryPublicId: string) =>
  api.get<TourResponse[]>(`/admin/galleries/${galleryPublicId}/tours`)

export const getTour = (galleryPublicId: string, tourPublicId: string) =>
  api.get<TourResponse>(`/admin/galleries/${galleryPublicId}/tours/${tourPublicId}`)

export const createTour = (galleryPublicId: string, data: object) =>
  api.post<TourResponse>(`/admin/galleries/${galleryPublicId}/tours`, data)

export const updateTour = (galleryPublicId: string, tourPublicId: string, data: object) =>
  api.put<TourResponse>(`/admin/galleries/${galleryPublicId}/tours/${tourPublicId}`, data)

export const deleteTour = (galleryPublicId: string, tourPublicId: string) =>
  api.delete(`/admin/galleries/${galleryPublicId}/tours/${tourPublicId}`)

export const reorderTours = (galleryPublicId: string, items: Array<{ publicId: string; sortOrder: number }>) =>
  api.put(`/admin/galleries/${galleryPublicId}/tours/reorder`, items)

// --- Stats ---
export const getGalleryStats = (from: string, to: string) =>
  api.get<ViewStatsResponse[]>('/admin/stats/galleries', { params: { from, to } })

export const getTourStats = (from: string, to: string, galleryPublicId?: string) =>
  api.get<ViewStatsResponse[]>('/admin/stats/tours', { params: { from, to, galleryPublicId } })

export const getImageStats = (from: string, to: string) =>
  api.get<ViewStatsResponse[]>('/admin/stats/images', { params: { from, to } })

// --- Themes ---
export const getThemes = () =>
  api.get<ThemeOption[]>('/public/themes')
