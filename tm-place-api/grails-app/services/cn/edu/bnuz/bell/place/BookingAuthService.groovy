package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.http.NotFoundException
import cn.edu.bnuz.bell.organization.Department
import cn.edu.bnuz.bell.organization.Teacher
import grails.transaction.Transactional

@Transactional
class BookingAuthService {

    def list() {
        def departments = Department.executeQuery '''
SELECT new map(
  id as id,
  name as name,
  isTeaching as isTeaching
)
from Department
where enabled = true
order by isTeaching desc, id
'''
        def types = BookingType.executeQuery '''
select new map (
  id as id,
  name as name,
  isTeaching as isTeaching
)
from BookingType
order by id
'''
        def auths = BookingAuth.executeQuery '''
select new map (
  auth.id as id,
  auth.department.id as departmentId,
  type.id as typeId,
  type.name as typeName,
  checker.id as checkerId,
  checker.name as checkerName,
  user.officePhone as checkerPhone
)
from BookingAuth auth
join auth.checker checker
join auth.type type
left join User user with user.id = checker.id 
'''
        return [
                departments: departments,
                types      : types,
                auths      : auths,
        ]
    }

    def getItemForCreate(String departmentId) {
        return [
                teachers: getDepartmentTeachers(departmentId),
        ]
    }

    def getItemForEdit(Long id) {
        def results = BookingAuth.executeQuery '''
select new map (
  department.id as departmentId,
  department.name as departmentName,
  type.id as typeId,
  type.name as typeName,
  checker.id as checkerId,
  checker.name as checkerName,
  user.officePhone as checkerPhone
)
from BookingAuth auth
join auth.department department
join auth.checker checker
join auth.type type
left join User user with user.id = checker.id
where auth.id = :id
''', [id: id]

        if (!results) {
            throw new NotFoundException()
        }

        def item = results[0]

        def types = BookingType.executeQuery '''
select new map (
  type.id as id,
  type.name as name
)
from BookingType type
where type.isTeaching = (
  select isTeaching from Department where id = :departmentId
)
and type.id not in (
  select ba.type.id from BookingAuth ba where ba.department.id = :departmentId
)
order by type.id
''', [departmentId : item.departmentId]

        def teachers = Teacher.executeQuery '''
select new map (
  teacher.id as id,
  teacher.name as name,
  user.officePhone as phone
)
from Teacher teacher
left join User user with user.id = teacher.id
where teacher.department.id = :departmentId
and teacher.atSchool = true
and teacher.isExternal = false
order by teacher.id
''', [departmentId: item.departmentId]

        return [
                item: item,
                types: types,
                teachers: teachers,
        ]

    }

    def save(BookingAuthCommand cmd) {
        def auth = new BookingAuth(
                department: Department.load(cmd.departmentId),
                type: BookingType.load(cmd.typeId),
                checker: Teacher.load(cmd.checkerId),
        )
        auth.save()
    }

    def update(BookingAuthCommand cmd) {
        def auth = BookingAuth.get(cmd.id)
        auth.type = BookingType.load(cmd.typeId)
        auth.checker = Teacher.load(cmd.checkerId)
        auth.save()
    }

    def delete(Long id) {
        def auth = BookingAuth.load(id)
        auth.delete()
    }

    def getDepartmentTeachers(String departmentId) {
        Teacher.executeQuery '''
select new map (
  teacher.id as id,
  teacher.name as name,
  user.officePhone as phone
)
from Teacher teacher
left join User user with user.id = teacher.id
where teacher.department.id = :departmentId
and teacher.atSchool = true
and teacher.isExternal = false
order by teacher.id
''', [departmentId: departmentId]
    }
}
