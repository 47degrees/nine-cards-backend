package cards.nine.services.free.interpreter.country

import cards.nine.commons.NineCardsErrors.CountryNotFound
import cards.nine.domain.pagination.Page
import cards.nine.services.common.PersistenceService
import cards.nine.services.common.PersistenceService._
import cards.nine.services.free.algebra.Country._
import cards.nine.services.free.domain.Country
import cards.nine.services.free.domain.Country.Queries
import cards.nine.services.persistence.Persistence
import cats.syntax.either._
import cats.~>
import doobie.imports._

class Services(persistence: Persistence[Country]) extends (Ops ~> ConnectionIO) {

  def getCountries(pageParams: Page): PersistenceService[List[Country]] =
    PersistenceService {
      persistence.fetchList(
        sql    = Queries.getCountriesWithPaginationSql,
        values = (pageParams.pageSize, pageParams.pageNumber)
      )
    }

  def getCountryByIsoCode2(isoCode: String): PersistenceService[Country] =
    persistence.fetchOption(Queries.getByIsoCode2Sql, isoCode.toUpperCase) map {
      Either.fromOption(_, CountryNotFound(s"Country with ISO code2 $isoCode doesn't exist"))
    }

  def apply[A](fa: Ops[A]): ConnectionIO[A] = fa match {
    case GetCountries(pageParams) ⇒ getCountries(pageParams)
    case GetCountryByIsoCode2(isoCode) ⇒ getCountryByIsoCode2(isoCode)
  }
}

object Services {

  def services(implicit persistence: Persistence[Country]) = new Services(persistence)
}
