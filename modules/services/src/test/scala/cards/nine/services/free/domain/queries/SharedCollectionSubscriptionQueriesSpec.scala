package cards.nine.services.free.domain.queries

import cards.nine.services.free.domain.SharedCollectionSubscription.Queries._
import cards.nine.services.persistence.DomainDatabaseContext
import doobie.contrib.specs2.analysisspec.AnalysisSpec
import org.specs2.mutable.Specification

class SharedCollectionSubscriptionQueriesSpec
  extends Specification
  with AnalysisSpec
  with DomainDatabaseContext {

  val collectionId = 12345l
  val publicIdentifier = "40daf308-fecf-4228-9262-a712d783cf49"
  val userId = 34567l

  val getByCollectionQuery = collectionSubscriptionPersistence.generateQuery(
    sql    = getByCollection,
    values = collectionId
  )
  check(getByCollectionQuery)

  val getByCollectionAndUserQuery = collectionSubscriptionPersistence.generateQuery(
    sql    = getByCollectionAndUser,
    values = (collectionId, userId)
  )
  check(getByCollectionAndUserQuery)

  val getByUserQuery = collectionSubscriptionPersistence.generateQuery(
    sql    = getByUser,
    values = userId
  )
  check(getByUserQuery)

  val insertQuery = collectionSubscriptionPersistence.generateUpdateWithGeneratedKeys(
    sql    = insert,
    values = (collectionId, userId, publicIdentifier)
  )
  check(insertQuery)

  val deleteByCollectionAndUserQuery = collectionSubscriptionPersistence.generateUpdateWithGeneratedKeys(
    sql    = deleteByCollectionAndUser,
    values = (collectionId, userId)
  )
  check(deleteByCollectionAndUserQuery)
}