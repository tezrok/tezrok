package io.tezrok.core

import io.tezrok.api.TezrokFeature
import io.tezrok.api.input.EntityElem
import io.tezrok.api.input.FieldElem
import io.tezrok.api.input.MethodProps

abstract class BaseTezrokFeature : TezrokFeature {
    protected fun mergeFields(inheritEntity: EntityElem?, fields: List<FieldElem>): List<FieldElem> {
        val map = inheritEntity?.fields?.associateBy { it.name }?.toMutableMap() ?: mutableMapOf()

        fields.forEach { field -> map[field.name] = inheritProperties(field, map[field.name]) }

        return map.values.toList()
    }

    protected fun inheritProperties(field: FieldElem, fieldFrom: FieldElem?): FieldElem {
        if (fieldFrom == null) {
            return field
        }

        return field.copy(
            type = field.type ?: fieldFrom.type,
            foreignField = field.foreignField ?: fieldFrom.foreignField,
            description = field.description ?: fieldFrom.description,
            required = field.required ?: fieldFrom.required,
            serial = field.serial ?: fieldFrom.serial,
            primary = field.primary ?: fieldFrom.primary,
            primaryIdFrom = field.primaryIdFrom ?: fieldFrom.primaryIdFrom,
            pattern = field.pattern ?: fieldFrom.pattern,
            minLength = field.minLength ?: fieldFrom.minLength,
            maxLength = field.maxLength ?: fieldFrom.maxLength,
            unique = field.unique ?: fieldFrom.unique,
            uniqueGroup = field.uniqueGroup ?: fieldFrom.uniqueGroup,
            defValue = field.defValue ?: fieldFrom.defValue,
            relation = field.relation ?: fieldFrom.relation
        )
    }

    protected fun applyAdminRole(inheritProps: MethodProps?): MethodProps {
        return (inheritProps ?: MethodProps()).copy(roles = inheritProps?.roles ?: listOf("ROLE_ADMIN"))
    }
}
