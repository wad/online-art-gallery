export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface ImageResponse {
  publicId: string
  shortId: string
  url: string
  thumbnailUrl: string
  title: string | null
  artistName: string | null
  description: string | null
  artCreationDate: string | null
  artistComments: string | null
  notes: string | null
  adminNotes: string | null
  nsfw: boolean
  uploadedAt: string
  baseImagePublicId: string | null
  tagNames: string[]
  galleryPublicIds: string[]
}

export interface TagResponse {
  publicId: string
  name: string
  imageCount: number
}

export interface BioLink {
  label: string
  url: string
}

export type GalleryTheme =
  | 'LIGHT' | 'DARK' | 'PASTEL' | 'SPRING' | 'WINTER'
  | 'CYBERPUNK' | 'SUNSET' | 'OCEAN' | 'MONOCHROME'

export type BorderStyle =
  | 'THEME_DEFAULT' | 'NONE' | 'THIN_LINE' | 'DOUBLE_LINE'
  | 'SHADOW' | 'ROUNDED' | 'ORNATE_FRAME' | 'POLAROID'

export interface GalleryResponse {
  publicId: string
  code: string
  name: string
  subtitle: string | null
  description: string | null
  headlinerImagePublicId: string | null
  isDefault: boolean
  visible: boolean
  showAllImagesTour: boolean
  sortOrder: number
  bioPhotoUrl: string | null
  bioText: string | null
  bioLinks: BioLink[]
  theme: GalleryTheme
  borderStyle: BorderStyle | null
  adsEnabled: boolean
  adLandingBanner: boolean
  adLandingBannerSlot: string | null
  adImageDetailSidebar: boolean
  adImageDetailSidebarSlot: string | null
}

export interface TourResponse {
  publicId: string
  name: string
  description: string | null
  galleryPublicId: string
  headlinerImagePublicId: string | null
  sortOrder: number
  tagPublicIds: string[]
  imagePublicIds: string[]
}

export interface ImageUsageResponse {
  galleries: Array<{
    galleryPublicId: string
    galleryName: string
    galleryCode: string
    tours: Array<{ tourPublicId: string; tourName: string }>
  }>
}

export interface AsyncJobResponse {
  jobId: string
  type: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED'
  totalItems: number | null
  processedItems: number
  failedItems: number
  errorMessages: string[]
  startedAt: string
  completedAt: string | null
}

export interface ViewStatsResponse {
  entityName: string
  entityCode: string | null
  entityShortId: string | null
  totalViews: number
  directViews: number
  tourViews: number
  dailyCounts: Array<{ date: string; count: number }>
}

export interface ThemeOption {
  value: string
  displayName: string
}

export interface PublicGalleryResponse {
  publicId: string
  code: string
  name: string
  subtitle: string | null
  description: string | null
  headlinerImagePublicId: string | null
  theme: GalleryTheme
  borderStyle: BorderStyle | null
  showAllImagesTour: boolean
  hasNsfwContent: boolean
  adsEnabled: boolean
  adLandingBanner: boolean
  adLandingBannerSlot: string | null
  adImageDetailSidebar: boolean
  adImageDetailSidebarSlot: string | null
  tours: TourSummary[]
  bioPhotoUrl: string | null
  bioText: string | null
  bioLinks: BioLink[]
}

export interface TourSummary {
  publicId: string
  name: string
  description: string | null
  headlinerImagePublicId: string | null
  sortOrder: number
}
