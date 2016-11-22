package cards.nine.processes

import cards.nine.commons.NineCardsService
import cards.nine.commons.NineCardsService._
import cards.nine.domain.account.AndroidId
import cards.nine.domain.application.{ CardList, FullCard, Package }
import cards.nine.domain.market.{ Localization, MarketCredentials, MarketToken }
import cards.nine.processes.NineCardsServices._
import cards.nine.services.free.algebra.GooglePlay.Services
import org.specs2.matcher.Matchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait ApplicationProcessesSpecification
  extends Specification
  with Matchers
  with Mockito
  with ApplicationProcessesContext
  with TestInterpreters {

  trait BasicScope extends Scope {

    implicit val googlePlayServices: Services[NineCardsServices] = mock[Services[NineCardsServices]]
    implicit val applicationProcesses = new ApplicationProcesses[NineCardsServices]

  }
}

trait ApplicationProcessesContext {

  val packagesName = List(
    "earth.europe.italy",
    "earth.europe.unitedKingdom",
    "earth.europe.germany",
    "earth.europe.france",
    "earth.europe.portugal",
    "earth.europe.spain"
  ) map Package

  val title = "Title of the app"

  val free = true

  val icon = "path-to-icon"

  val stars = 4.5

  val downloads = "1000000+"

  val category = "SOCIAL"

  val (missing, found) = packagesName.partition(_.value.length > 6)

  val apps = found map (packageName ⇒ FullCard(packageName, title, List(category), downloads, free, icon, Nil, stars))

  val androidId = "12345"
  val localization = "en_GB"
  val token = "m52_9876"

  val marketAuth = MarketCredentials(AndroidId(androidId), MarketToken(token), Some(Localization(localization)))

  val emptyGetAppsInfoResponse = CardList(Nil, Nil)

  val appsInfo = CardList(missing, apps)
}

class ApplicationProcessesSpec extends ApplicationProcessesSpecification {

  "getAppsInfo" should {
    "return an empty response without calling the Google Play service if an empty list of" +
      "packages name is passed" in new BasicScope {

        applicationProcesses.getAppsInfo(Nil, marketAuth).foldMap(testInterpreters) must beRight(emptyGetAppsInfoResponse)
      }

    "return a valid response if a non empty list of packages name is passed" in new BasicScope {

      googlePlayServices.resolveManyDetailed(packagesName, marketAuth) returns
        NineCardsService.right(appsInfo)

      applicationProcesses
        .getAppsInfo(packagesName, marketAuth)
        .foldMap(testInterpreters) must beRight[CardList[FullCard]].which { response ⇒
          response.missing must_== missing
          response.cards must_== apps
        }
    }
  }
}
