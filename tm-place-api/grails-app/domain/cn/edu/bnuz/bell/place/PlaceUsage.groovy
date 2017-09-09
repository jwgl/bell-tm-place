package cn.edu.bnuz.bell.place

import cn.edu.bnuz.bell.master.Term
import org.codehaus.groovy.util.HashCodeHelper

class PlaceUsage implements Serializable {
    Term term
    Place place
    Integer startWeek
    Integer endWeek
    Integer oddEven
    Integer dayOfWeek
    Integer startSection
    Integer totalSection
    String type
    String department
    String description

    static mapping = {
        table 'dv_place_usage'
        id composite: [
                'term',
                'place',
                'startWeek',
                'endWeek',
                'oddEven',
                'dayOfWeek',
                'startSection',
                'totalSection',
                'type',
                'department',
                'description',
        ]
    }

    boolean equals(other) {
        if (!(other instanceof PlaceUsage)) {
            return false
        }

        other.term?.id == term?.id &&
                other.place?.id == place?.id &&
                other.startWeek == startWeek &&
                other.endWeek == endWeek &&
                other.oddEven == oddEven &&
                other.dayOfWeek == dayOfWeek &&
                other.startSection == startSection &&
                other.totalSection == totalSection &&
                other.type == type &&
                other.department == department &&
                other.description == description
    }

    int hashCode() {
        int hash = HashCodeHelper.initHash()
        hash = HashCodeHelper.updateHash(hash, term.id)
        hash = HashCodeHelper.updateHash(hash, place.id)
        hash = HashCodeHelper.updateHash(hash, startWeek)
        hash = HashCodeHelper.updateHash(hash, endWeek)
        hash = HashCodeHelper.updateHash(hash, oddEven)
        hash = HashCodeHelper.updateHash(hash, dayOfWeek)
        hash = HashCodeHelper.updateHash(hash, startSection)
        hash = HashCodeHelper.updateHash(hash, totalSection)
        hash = HashCodeHelper.updateHash(hash, type)
        hash = HashCodeHelper.updateHash(hash, department)
        hash = HashCodeHelper.updateHash(hash, description)
        hash
    }
}
