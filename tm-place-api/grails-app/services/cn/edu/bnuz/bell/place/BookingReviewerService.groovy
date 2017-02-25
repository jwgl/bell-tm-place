package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.workflow.Activities
import cn.edu.bnuz.bell.workflow.ReviewerProvider
import grails.transaction.Transactional

@Transactional(readOnly = true)
class BookingReviewerService implements ReviewerProvider{
    List<Map> getReviewers(Object id, String activity) {
        switch (activity) {
            case Activities.CHECK:
                return getCheckers(id as Long)
            case Activities.APPROVE:
                return getApprovers()
            default:
                throw new BadRequestException()
        }
    }

    def getCheckers(Long id) {
        BookingAuth.executeQuery '''
select new map(
  checker.id as id,
  checker.name as name
)
from BookingAuth bc
join bc.checker checker
where (bc.type, bc.department) in (
  select form.type, form.department
  from BookingForm form
  where form.id = :id
)''', [id: id]
    }

    def getApprovers() {
        User.findAllWithPermission('PERM_PLACE_BOOKING_APPROVE')
    }
}
