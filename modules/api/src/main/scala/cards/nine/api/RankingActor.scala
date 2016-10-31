package cards.nine.api

import akka.actor.{Actor, ActorSystem}
import akka.event.Logging
import cards.nine.api.RankingActor.RankingByCategory
import cards.nine.commons.NineCardsService.Result
import cards.nine.commons.TaskInstances._
import cards.nine.commons.config.NineCardsConfig._
import cards.nine.domain.analytics._
import cards.nine.domain.pagination.Page
import cards.nine.processes.RankingProcesses
import cards.nine.processes.messages.rankings.Reload.SummaryResponse
import cats.~>
import doobie.enum.resultsetconcurrency.ResultSetConcurrency
import org.joda.time.{DateTime, DateTimeZone}

import scalaz.{-\/, \/, \/-}
import scalaz.concurrent.Task

class RankingActor[F[_]](interpreter: F ~> Task)(implicit rankingProcesses: RankingProcesses[F]) extends Actor {
  implicit val system = ActorSystem("on-spray-can")
  val log = Logging(system, getClass)

  private[this] def generateRankings = {
    val countriesPerRequest = nineCardsConfiguration.rankings.countriesPerRequest
    val maxNumberOfAppsPerCategory = nineCardsConfiguration.rankings.maxNumberOfAppsPerCategory
    val rankingPeriod = nineCardsConfiguration.rankings.rankingPeriod

    val now = DateTime.now(DateTimeZone.UTC)
    val today = now.withTimeAtStartOfDay

    val pageNumber = ((now.getDayOfWeek - 1) * 24 + now.getHourOfDay) * countriesPerRequest

    rankingProcesses.reloadRankingForCountries(
      RankingParams(
        DateRange(
          today.minus(rankingPeriod),
          today
        ),
        maxNumberOfAppsPerCategory,
        AnalyticsToken("") //TODO: We should generate a valid token
      ),
      Page(pageNumber, countriesPerRequest)
    )
  }

  private[this] def showRankingGenerationInfo(result: Throwable \/ Result[SummaryResponse]) = {
    result match {
      case -\/(e) => log.error(e, "An error was found while generating rankings")
      case \/-(Left(e)) => log.error("An error was found while generating rankings")
      case \/-(Right(summary)) =>
        showCountriesWithoutRankingInfo(summary.countriesWithoutRanking)
        summary.countriesWithRanking foreach showRankingSummary
        log.info("Rankings generated successfully")
    }
  }

  private[this] def showCountriesWithoutRankingInfo(countriesCode: List[CountryIsoCode]) =
    if (countriesCode.nonEmpty)
      log.info(s"Skipped countries without ranking info: ${countriesCode.map(_.value).mkString(",")}")

  private[this] def showRankingSummary(summary: UpdateRankingSummary) =
    summary.countryCode match {
      case None ⇒ log.info(s"World ranking generated: ${summary.created} entries created")
      case Some(code) ⇒ log.info(s"Ranking generated for ${code.value}: ${summary.created} entries created")
    }

  def receive = {
    case RankingByCategory ⇒
      log.info("Running actor for generate rankings ...")
      generateRankings.foldMap(interpreter).unsafePerformAsync(showRankingGenerationInfo)
  }
}

object RankingActor {
  val RankingByCategory = "ranking"
}