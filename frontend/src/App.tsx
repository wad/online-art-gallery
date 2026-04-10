import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'

// Admin
import RequireAuth from './admin/components/RequireAuth'
import AdminLayout from './admin/components/AdminLayout'
import LoginPage from './admin/pages/LoginPage'
import DashboardPage from './admin/pages/DashboardPage'
import PasswordPage from './admin/pages/PasswordPage'
import ImagesPage from './admin/pages/ImagesPage'
import ImageDetailPage from './admin/pages/ImageDetailPage'
import BulkLoadPage from './admin/pages/BulkLoadPage'
import GalleriesPage from './admin/pages/GalleriesPage'
import GalleryFormPage from './admin/pages/GalleryFormPage'
import TagsPage from './admin/pages/TagsPage'
import StatsPage from './admin/pages/StatsPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Admin auth */}
        <Route path="/admin/login" element={<LoginPage />} />

        {/* Protected admin routes */}
        <Route element={<RequireAuth />}>
          <Route element={<AdminLayout />}>
            <Route path="/admin" element={<DashboardPage />} />
            <Route path="/admin/images" element={<ImagesPage />} />
            <Route path="/admin/images/bulk" element={<BulkLoadPage />} />
            <Route path="/admin/images/new" element={<ImageDetailPage />} />
            <Route path="/admin/images/:publicId" element={<ImageDetailPage />} />
            <Route path="/admin/galleries" element={<GalleriesPage />} />
            <Route path="/admin/galleries/new" element={<GalleryFormPage />} />
            <Route path="/admin/galleries/:publicId" element={<GalleryFormPage />} />
            <Route path="/admin/tags" element={<TagsPage />} />
            <Route path="/admin/stats" element={<StatsPage />} />
            <Route path="/admin/password" element={<PasswordPage />} />
          </Route>
        </Route>

        {/* Visitor routes — placeholder until Phase 10 */}
        <Route path="/" element={<div style={{ padding: 40, fontFamily: 'sans-serif' }}>Gallery loading…</div>} />
        <Route path="/galleries" element={<div style={{ padding: 40, fontFamily: 'sans-serif' }}>Galleries page coming soon.</div>} />
        <Route path="/:code" element={<div style={{ padding: 40, fontFamily: 'sans-serif' }}>Gallery page coming soon.</div>} />
        <Route path="/:code/*" element={<div style={{ padding: 40, fontFamily: 'sans-serif' }}>Gallery page coming soon.</div>} />

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/admin" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
