package cards.nine.services.free.domain

object GooglePlay {

  case class PackageList(items: Seq[String]) extends AnyVal

  case class AuthParams(
    androidId: String,
    localization: Option[String],
    token: String
  )

  case class AppsInfo(
    missing: List[String],
    apps: List[AppInfo]
  )

  case class AppInfo(
    packageName: String,
    title: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    categories: List[String]
  )

  case class Recommendations(
    apps: List[Recommendation]
  )

  case class Recommendation(
    packageName: String,
    name: String,
    free: Boolean,
    icon: String,
    stars: Double,
    downloads: String,
    screenshots: List[String]
  )
}
