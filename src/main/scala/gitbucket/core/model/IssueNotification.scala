package gitbucket.core.model

trait IssueNotificationComponent extends TemplateComponent { self: Profile =>
  import profile.api._

  lazy val IssueNotifications = TableQuery[IssueNotifications]

  class IssueNotifications(tag: Tag) extends Table[IssueNotification](tag, "ISSUE_NOTIFICATION") with IssueTemplate {
    val notificationUserName = column[String]("NOTIFICATION_USER_NAME")
    val subscribed = column[Boolean]("SUBSCRIBED")
    def * = (userName, repositoryName, issueId, notificationUserName, subscribed) <> (IssueNotification.tupled, IssueNotification.unapply)

    def byPrimaryKey(owner: String, repository: String, issueId: Int, notificationUser: String) =
      byIssue(owner, repository, issueId) && (notificationUserName === notificationUser.bind)
  }
}

case class IssueNotification(
  userName: String,
  repositoryName: String,
  issueId: Int,
  notificationUserName: String,
  subscribed: Boolean
)
