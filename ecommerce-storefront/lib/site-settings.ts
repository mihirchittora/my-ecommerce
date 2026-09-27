import { catalogApi } from "@/lib/api/catalog";
import { siteTitleOrDefault } from "@/lib/utils";

export async function getConfiguredSiteTitle() {
  try {
    return siteTitleOrDefault((await catalogApi.getSiteSettings()).siteTitle);
  } catch {
    return siteTitleOrDefault(undefined);
  }
}
