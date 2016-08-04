package com.fortysevendeg.ninecards.services.persistence

import com.fortysevendeg.ninecards.services.free.domain._
import com.fortysevendeg.ninecards.services.persistence.SharedCollectionPersistenceServices.SharedCollectionData
import com.fortysevendeg.ninecards.services.persistence.UserPersistenceServices.UserData
import doobie.imports._
import org.specs2.ScalaCheck
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import scala.annotation.tailrec
import scala.collection.immutable.List

import scalaz.syntax.traverse.ToTraverseOps // F[A] => TraverseOps[F, A]
import scalaz.std.list.listInstance // Traverse[List]

import shapeless.syntax.std.product._

class SharedCollectionPersistenceServicesSpec
  extends Specification
  with BeforeEach
  with ScalaCheck
  with DomainDatabaseContext
  with DisjunctionMatchers
  with NineCardsScalacheckGen {

  sequential

  def before = {
    flywaydb.clean()
    flywaydb.migrate()
  }

  "addCollection" should {
    "create a new shared collection when an existing user id is given" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          u ← createUser(userData)
          c ← sharedCollectionPersistenceServices.addCollection[Long](
            collectionData.copy(userId = Option(u))
          )
        } yield c).transact(transactor).run

        val storedCollection = sharedCollectionPersistenceServices.getCollectionById(
          id = id
        ).transact(transactor).run

        storedCollection must beSome[SharedCollection].which {
          collection ⇒ collection.publicIdentifier must_== collectionData.publicIdentifier
        }
      }
    }

    "create a new shared collection without a defined user id" in {
      prop { (collectionData: SharedCollectionData) ⇒
        val id: Long = sharedCollectionPersistenceServices.addCollection[Long](
          collectionData.copy(userId = None)
        ).transact(transactor).run

        val storedCollection = sharedCollectionPersistenceServices.getCollectionById(
          id = id
        ).transact(transactor).run

        storedCollection must beSome[SharedCollection].which {
          collection ⇒ collection.publicIdentifier must_== collectionData.publicIdentifier
        }
      }
    }
  }

  "getCollectionById" should {
    "return None if the table is empty" in {
      prop { (id: Long) ⇒
        val collection = sharedCollectionPersistenceServices.getCollectionById(
          id = id
        ).transact(transactor).run

        collection must beNone
      }
    }
    "return a collection if there is a record with the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transact(transactor).run

        val storedCollection = sharedCollectionPersistenceServices.getCollectionById(
          id = id
        ).transact(transactor).run

        storedCollection must beSome[SharedCollection].which {
          collection ⇒ collection.publicIdentifier must_== collectionData.publicIdentifier
        }
      }
    }
    "return None if there isn't any collection with the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transact(transactor).run

        val storedCollection = sharedCollectionPersistenceServices.getCollectionById(
          id = id + 1000000
        ).transact(transactor).run

        storedCollection must beNone
      }
    }
  }

  "getCollectionByPublicIdentifier" should {
    "return None if the table is empty" in {
      prop { (publicIdentifier: String) ⇒

        val collection = sharedCollectionPersistenceServices.getCollectionByPublicIdentifier(
          publicIdentifier = publicIdentifier
        ).transact(transactor).run

        collection must beNone
      }
    }
    "return a collection if there is a record with the given public identifier in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transact(transactor).run

        val storedCollection = sharedCollectionPersistenceServices.getCollectionByPublicIdentifier(
          publicIdentifier = collectionData.publicIdentifier
        ).transact(transactor).run

        storedCollection must beSome[SharedCollection].which {
          collection ⇒
            collection.id must_== id
            collection.publicIdentifier must_== collectionData.publicIdentifier
        }
      }
    }
    "return None if there isn't any collection with the given public identifier in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData) ⇒
        val id = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transact(transactor).run

        val collection = sharedCollectionPersistenceServices.getCollectionByPublicIdentifier(
          publicIdentifier = collectionData.publicIdentifier.reverse
        ).transact(transactor).run

        collection must beNone
      }
    }
  }

  "getCollectionsByUserId" should {

    def divideList[A](n: Int, list: List[A]): List[List[A]] = {
      def merge(heads: List[A], tails: List[List[A]]): List[List[A]] = (heads, tails) match {
        case (Nil, Nil) ⇒ Nil
        case (h :: hs, Nil) ⇒ throw new Exception("This should not happen")
        case (Nil, t :: ts) ⇒ tails
        case (h :: hs, t :: ts) ⇒ (h :: t) :: merge(hs, ts)
      }

      @tailrec
      def divideAux(xs: List[A], results: List[List[A]]): List[List[A]] =
        if (xs.isEmpty)
          results map (_.reverse)
        else {
          val (pre, post) = xs.splitAt(n)
          divideAux(post, merge(pre, results))
        }

      divideAux(list, List.fill(n)(Nil))
    }

    "return the List of Collections created by the User" in {

      prop { (ownerData: UserData, otherData: UserData, collectionData: List[SharedCollectionData]) ⇒
        val List(ownedData, disownedData, foreignData) = divideList[SharedCollectionData](3, collectionData)

        val setupTrans = for {
          ownerId ← createUser(ownerData)
          otherId ← createUser(otherData)
          owned ← ownedData traverse (createCollectionWithUser(_, Option(ownerId)))
          foreign ← foreignData traverse (createCollectionWithUser(_, Option(otherId)))
          disowned ← disownedData traverse (createCollectionWithUser(_, None))
        } yield (ownerId, otherId, owned, disowned, foreign)

        val (ownerId, otherId, owned, disowned, foreign) = setupTrans.transact(transactor).run

        val response: List[SharedCollection] =
          sharedCollectionPersistenceServices
            .getCollectionsByUserId(ownerId)
            .transact(transactor).run

        (response map (_.id)) must containTheSameElementsAs(owned)
      }
    }
  }

  def createCollectionWithUser(collectionData: SharedCollectionData, userId: Option[Long]): ConnectionIO[Long] =
    insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = userId))

  def createUser(userData: UserData): ConnectionIO[Long] =
    insertItem(User.Queries.insert, userData.toTuple)

  def createPackages(collectionId: Long, packageNames: List[String]): ConnectionIO[Int] =
    insertItems(SharedCollectionPackage.Queries.insert, packageNames map { (collectionId, _) })

  "addPackage" should {
    "create a new package associated with an existing shared collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, packageName: String) ⇒
        val collectionId = (for {
          u ← createUser(userData)
          c ← createCollectionWithUser(collectionData, Option(u))
        } yield c).transact(transactor).run

        val packageId = sharedCollectionPersistenceServices.addPackage[Long](
          collectionId,
          packageName
        ).transact(transactor).run

        val storedPackages = sharedCollectionPersistenceServices.getPackagesByCollection(
          collectionId
        ).transact(transactor).run

        storedPackages must contain { p: SharedCollectionPackage ⇒
          p.id must_== packageId
        }.atMostOnce
      }
    }
  }

  "addPackages" should {
    "create new packages associated with an existing shared collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, packagesName: List[String]) ⇒
        val collectionId = (for {
          u ← createUser(userData)
          c ← createCollectionWithUser(collectionData, Option(u))
        } yield c).transact(transactor).run

        val created = sharedCollectionPersistenceServices.addPackages(
          collectionId,
          packagesName
        ).transact(transactor).run

        created must_== packagesName.size
      }
    }
  }

  "getPackagesByCollection" should {
    "return an empty list if the table is empty" in {
      prop { (collectionId: Long) ⇒
        val packages = sharedCollectionPersistenceServices.getPackagesByCollection(
          collectionId
        ).transact(transactor).run

        packages must beEmpty
      }
    }
    "return a list of packages associated with the given shared collection" in {

      prop { (userData: UserData, collectionData: SharedCollectionData, packagesName: List[String]) ⇒
        val collectionId = (for {
          u ← createUser(userData)
          c ← createCollectionWithUser(collectionData, Option(u))
          _ ← createPackages(c, packagesName)
        } yield c).transact(transactor).run

        val packages = sharedCollectionPersistenceServices.getPackagesByCollection(
          collectionId
        ).transact(transactor).run

        packages must haveSize(packagesName.size)

        packages must contain { p: SharedCollectionPackage ⇒
          p.sharedCollectionId must_=== collectionId
        }.forall
      }
    }
    "return an empty list if there isn't any package associated with the given collection" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, packagesName: List[String]) ⇒
        val collectionId = (for {
          u ← createUser(userData)
          c ← createCollectionWithUser(collectionData, Option(u))
          _ ← createPackages(c, packagesName)
        } yield c).transact(transactor).run

        val packages = sharedCollectionPersistenceServices.getPackagesByCollection(
          collectionId + 1000000
        ).transact(transactor).run

        packages must beEmpty
      }
    }
  }

  "updateCollection" should {
    "return 0 updated rows if the table is empty" in {
      prop { (id: Long, title: String, description: Option[String]) ⇒
        val updatedCollectionCount = sharedCollectionPersistenceServices.updateCollectionInfo(
          id          = id,
          title       = title,
          description = description
        ).transact(transactor).run

        updatedCollectionCount must_== 0
      }
    }
    "return 1 updated row if there is a collection with the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, newTitle: String, newDescription: Option[String]) ⇒
        val id = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
          _ ← sharedCollectionPersistenceServices.updateCollectionInfo(c, newTitle, newDescription)
        } yield c).transact(transactor).run

        val storedCollection = sharedCollectionPersistenceServices.getCollectionById(
          id = id
        ).transact(transactor).run

        storedCollection must beSome[SharedCollection].which {
          collection ⇒
            collection.name must_== newTitle
            collection.description must_== newDescription
        }
      }
    }
    "return 0 updated rows if there isn't any collection with the given id in the database" in {
      prop { (userData: UserData, collectionData: SharedCollectionData, newTitle: String, newDescription: Option[String]) ⇒
        val id = (for {
          u ← insertItem(User.Queries.insert, userData.toTuple)
          c ← insertItem(SharedCollection.Queries.insert, collectionData.copy(userId = Option(u)).toTuple)
        } yield c).transact(transactor).run

        val updatedCollectionCount = sharedCollectionPersistenceServices.updateCollectionInfo(
          id          = id + 1000000,
          title       = newTitle,
          description = newDescription
        ).transact(transactor).run

        updatedCollectionCount must_== 0
      }
    }
  }
}