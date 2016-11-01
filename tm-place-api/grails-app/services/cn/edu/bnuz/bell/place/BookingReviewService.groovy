package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.BadRequestException
import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.workflow.*
import cn.edu.bnuz.bell.workflow.commands.AcceptCommand
import cn.edu.bnuz.bell.workflow.commands.RejectCommand
import grails.transaction.Transactional

@Transactional
class BookingReviewService extends AbstractReviewService {
    BookingFormService bookingFormService
    DomainStateMachineHandler domainStateMachineHandler

    def getFormForReview(Long id, String userId, UUID workitemId) {
        def form = bookingFormService.getFormInfo(id)
        def activity = Workitem.get(workitemId).activitySuffix
        checkReviewer(id, activity, userId)
        form.activity = activity
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

        def activity = Workitem.get(workitemId).activitySuffix
        checkReviewer(cmd.id, activity, userId)

        switch (activity) {
            case Activities.CHECK:
                form.checker = Teacher.load(userId)
                form.dateChecked = new Date()
                break
            case Activities.APPROVE:
                form.approver = Teacher.load(userId)
                form.dateApproved = new Date()
                break
        }

        domainStateMachineHandler.accept(form, userId, cmd.comment, workitemId, cmd.to)

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

        def activity = Workitem.get(workitemId).activitySuffix
        checkReviewer(cmd.id, activity, userId)

        domainStateMachineHandler.reject(form, userId, cmd.comment, workitemId)

        form.save()
    }

    @Override
    List<Map> getReviewers(String activity, Long id) {
        switch (activity) {
            case Activities.CHECK:
                return bookingFormService.getCheckers(id)
            case Activities.APPROVE:
                return User.findAllWithPermission('PERM_PLACE_BOOKING_APPROVE')
            default:
                throw new BadRequestException()
        }
    }
}

