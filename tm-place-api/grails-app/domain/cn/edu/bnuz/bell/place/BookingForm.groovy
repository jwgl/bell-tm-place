package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.master.Term
import cn.edu.bnuz.bell.organization.Department
import cn.edu.bnuz.bell.organization.Teacher
import cn.edu.bnuz.bell.security.User
import cn.edu.bnuz.bell.workflow.IStateObject
import cn.edu.bnuz.bell.workflow.States
import cn.edu.bnuz.bell.workflow.WorkflowInstance

/**
 * 教室借用单
 */
class BookingForm implements IStateObject {
    /**
     * 学期
     */
    Term term

    /**
     * 借用单位
     */
    Department department

    /**
     * 借用类型
     */
    BookingType type

    /**
     * 借用理由
     */
    String reason

    /**
     * 状态
     */
    States status

    /**
     * 通知单
     */
    BookingReport report

    /**
     * 借用人
     */
    User user

    /**
     * 创建时间
     */
    Date dateCreated

    /**
     * 修改时间
     */
    Date dateModified

    /**
     * 审核人
     */
    Teacher checker

    /**
     * 审核时间
     */
    Date dateChecked

    /**
     * 审批人
     */
    Teacher approver

    /**
     * 审批时间
     */
    Date dateApproved

    /**
     * 工作流实例
     */
    WorkflowInstance workflowInstance

    static hasMany = [
        items : BookingItem
    ]

    static mapping = {
        comment          '教室借用单'
        id               generator: 'identity', comment: 'ID'
        term             comment: '学期'
        department       comment: '借用单位'
        type             comment: '借用类型'
        reason           comment: '借用理由'
        status           comment: '状态'
        report           comment: '通知单'
        user             comment: '借用人'
        dateCreated      comment: '创建时间'
        dateModified     comment: '修改时间'
        checker          comment: '审核人'
        dateChecked      comment: '审核时间'
        approver         comment: '审批人'
        dateApproved     comment: '审批时间'
        workflowInstance comment: '工作流实例'
    }

    static constraints = {
        reason           maxSize: 255
        report           nullable: true
        checker          nullable: true
        dateChecked      nullable: true
        approver         nullable: true
        dateApproved     nullable: true
        workflowInstance nullable: true
    }

    String getWorkflowId() {
        'place.booking'
    }
}
