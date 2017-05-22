package gitbucket.core.model

trait WatchComponent extends TemplateComponent { self: Profile =>
  import profile.api._

  lazy val Watches = TableQuery[Watches]

  class Watches(tag: Tag) extends Table[Watch](tag, "WATCH") with BasicTemplate {
    val notificationUserName = column[String]("NOTIFICATION_USER_NAME")
    val notification = column[String]("NOTIFICATION")
    def * = (userName, repositoryName, notificationUserName, notification) <> (Watch.tupled, Watch.unapply)

    def byPrimaryKey(owner: String, repository: String, notificationUser: String) =
      byRepository(owner, repository) && (notificationUserName === notificationUser.bind)
  }
}

case class Watch(
  userName: String,
  repositoryName: String,
  notificationUserName: String,
  notification: String  // not_watching or watching [or ignoring]    // TODO should be sealed class
)
