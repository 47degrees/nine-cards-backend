package cards.nine.services.persistence

import cards.nine.domain.application.{ Category, Package }
import cards.nine.services.free.domain.rankings.Entry
import doobie.imports._

object CustomComposite {
  implicit val packageName: Composite[Package] = Composite_Aux.packageName
  implicit val category: Composite[Category] = Composite_Aux.category
  implicit val rankingEntry: Composite[Entry] = Composite_Aux.rankingEntry
}

object Composite_Aux {

  private[this] val string: Composite[String] = implicitly[Composite[String]]

  implicit val packageName_a: Atom[Package] =
    Atom[String].xmap[Package](Package.apply, _.value)

  implicit val packageName: Composite[Package] =
    string.xmap[Package](Package.apply, _.value)

  implicit val category_a: Atom[Category] =
    Atom[String].xmap(Category.withName, _.entryName)

  implicit val category: Composite[Category] =
    string.xmap(Category.withName, _.entryName)

  val rankingEntry: Composite[Entry] = implicitly[Composite[Entry]]

}