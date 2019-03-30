package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.master.Term
import cn.edu.bnuz.bell.security.SecurityService
import cn.edu.bnuz.bell.security.UserType
import grails.gorm.transactions.Transactional

@Transactional(readOnly = true)
class PlaceService {
    SecurityService securityService

    def findPlaces(Term term, UserType userType, Boolean isAdmin, String building) {
        Place.executeQuery '''
select new map(
    place.id as id,
    place.name as name,
    place.seat as seat,
    place.type as type
)
from PlaceUserType placeUserType
join placeUserType.place place
left join place.allowBookingTerms placeBookingTerm with placeBookingTerm.term = :term
where (placeUserType.userType = :userType or :isAdmin = true)
and (place.enabled = true or placeBookingTerm.term is not null)
and building = :building
order by place.name
''', [
                term    : term,
                userType: userType,
                building: building,
                isAdmin : isAdmin
        ]
    }

    def getUsages(Term term, UserType userType, Boolean isAdmin, String placeId) {
        PlaceUsage.executeQuery '''
select new map(
    startWeek as startWeek,
    endWeek as endWeek,
    oddEven as oddEven,
    dayOfWeek as dayOfWeek,
    startSection as startSection,
    totalSection as totalSection,
    case
        when description like '%】' then 'tk'
        else type
    end as type,
    department as department,
    case
      when type = 'pk' then description
      when cast(:userType as int) = 1 and description not like '%】' then description
      else null
    end as description
)
from PlaceUsage placeUsage
where placeUsage.term = :term
and placeUsage.place.id = :placeId
and place in (
    select place
    from PlaceUserType placeUserType
    join placeUserType.place place
    left join place.allowBookingTerms placeBookingTerm with placeBookingTerm.term = :term
    where (placeUserType.userType = :userType or :isAdmin = true)
    and (place.enabled = true or placeBookingTerm.term is not null)
)
''', [term: term, userType: userType, isAdmin: isAdmin, placeId: placeId]
    }
}
