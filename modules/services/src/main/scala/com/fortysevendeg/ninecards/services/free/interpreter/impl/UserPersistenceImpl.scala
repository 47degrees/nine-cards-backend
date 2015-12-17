package com.fortysevendeg.ninecards.services.free.interpreter.impl

import com.fortysevendeg.ninecards.services.free.domain.{GoogleAuthDataDeviceInfo, GoogleAuthData, AuthData, User}

class UserPersistenceImpl {

  def addUser(user: User) = user

  def getUserByUserName(username: String) = Option(User())

  def checkPassword(pass: String) = true

  def getUserByUserId(userId: String) =
    Option(
      User(
        id = Option(userId),
        username = Option("Ana"),
        email = Option("ana@47deg.com"),
        sessionToken = Option("asjdfoaijera"),
        authData = Option(AuthData(
          google = Option(GoogleAuthData(
            email = "ana@47deg.com",
            devices = List(
              GoogleAuthDataDeviceInfo(
                name = "aldfa",
                deviceId = "ladf",
                secretToken = "lakjdsflkadf",
                permissions = Nil
              )
            ))
          ))
        )
      )
    )

  def getUserByEmail(email: String) =
    Option(
      User(
        id = Option("32132165"),
        username = Option("Ana"),
        email = Option(email),
        sessionToken = Option("asjdfoaijera"),
        authData = Option(AuthData(
          google = Option(GoogleAuthData(
            email = "ana@47deg.com",
            devices = List(
              GoogleAuthDataDeviceInfo(
                name = "aldfa",
                deviceId = "ladf",
                secretToken = "lakjdsflkadf",
                permissions = Nil
              )
            ))
          ))
        )
      )
    )

  //For QA, when not exist user in DB.
  //def getUserByEmail(email: String) = None

  def insertUserDB(user: User) = user

}

object UserPersistenceImpl {

  implicit def userPersistenceImpl = new UserPersistenceImpl()
}
