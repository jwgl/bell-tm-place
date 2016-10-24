package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.workflow.*
import grails.transaction.Transactional

@Transactional
class BookingReviewService extends AbstractReviewService {
    BookingFormService bookingFormService
    DomainStateMachineHandler domainStateMachineHandler

    def getFormForReview(Long id, String userId, UUID workitemId) {
        def form = bookingFormService.getFormInfo(id)
        def reviewType = Workitem.get(workitemId).activitySuffix
        checkReviewer(id, reviewType, userId)
        form.reviewType = reviewType
        return form
    }

    void accept(AcceptCommand cmd, String userId, UUID workitemId) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (!domainStateMachineHandler.canAccept(form)) {
            throw new BadRequestException()
        }

        def reviewType = Workitem.get(workitemId).activitySuffix
        checkReviewer(cmd.id, reviewType, userId)

        switch (reviewType) {
            case ReviewTypes.CHECK:
                form.checker = Teacher.load(userId)
                form.dateChecked = new Date()
                break
            case ReviewTypes.APPROVE:
                form.approver = Teacher.load(userId)
                form.dateApproved = new Date()
                break
        }

        domainStateMachineHandler.accept(userId, cmd.to, cmd.comment, form, workitemId)

        form.save()
    }

    void reject(RejectCommand cmd, String userId, UUID workitemId) {
        BookingForm form = BookingForm.get(cmd.id)

        if (!form) {
            throw new NotFoundException()
        }

        if (!domainStateMachineHandler.canReject(form)) {
            throw new BadRequestException()
        }

        def reviewType = Workitem.get(workitemId).activitySuffix
        checkReviewer(cmd.id, reviewType, userId)

        domainStateMachineHandler.reject(userId, cmd.comment, form, workitemId)

        form.save()
    }

    List<Map> getReviewers(String type, Long id) {
        switch (type) {
            case ReviewTypes.CHECK:
                return bookingFormService.getCheckers(id)
            case ReviewTypes.APPROVE:
                return User.findAllWithPermission('PERM_PLACE_BOOKING_APPROVE')
            default:
                throw new BadRequestException()
        }
    }
}

