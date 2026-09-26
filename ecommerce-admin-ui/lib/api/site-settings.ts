import { catalogClient } from "@/lib/api/client";
import type { CarouselSlide, SiteSettings } from "@/lib/types";

export type SiteSettingsPayload = { siteTitle: string };
export type CarouselSlidePayload = {
  headline: string;
  eyebrow?: string;
  description?: string;
  primaryCtaLabel?: string;
  primaryCtaUrl?: string;
  secondaryCtaLabel?: string;
  secondaryCtaUrl?: string;
  sortOrder: number;
  active: boolean;
};

export const siteSettingsApi = {
  get: () => catalogClient.request<SiteSettings>("/v1/site-settings/admin"),
  update: (payload: SiteSettingsPayload) => catalogClient.json<SiteSettings, SiteSettingsPayload>("/v1/site-settings", payload, { method: "PUT" }),
  uploadLogo: (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return catalogClient.request<SiteSettings>("/v1/site-settings/logo", { method: "POST", body: formData });
  },
  deleteLogo: () => catalogClient.request<SiteSettings>("/v1/site-settings/logo", { method: "DELETE" }),
  updateSlide: (slideId: string, payload: CarouselSlidePayload) => catalogClient.json<CarouselSlide, CarouselSlidePayload>(`/v1/site-settings/slides/${encodeURIComponent(slideId)}`, payload, { method: "PUT" }),
  uploadSlideImage: (slideId: string, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return catalogClient.request<CarouselSlide>(`/v1/site-settings/slides/${encodeURIComponent(slideId)}/image`, { method: "POST", body: formData });
  },
  deleteSlideImage: (slideId: string) => catalogClient.request<CarouselSlide>(`/v1/site-settings/slides/${encodeURIComponent(slideId)}/image`, { method: "DELETE" }),
};
