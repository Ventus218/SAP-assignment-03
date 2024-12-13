package users.domain.model;

case class User(username: Username, credit: Credit)

extension (u: User)
  def rechargeCredit(credit: Credit): User =
    u.copy(credit = Credit(u.credit.amount + credit.amount))
