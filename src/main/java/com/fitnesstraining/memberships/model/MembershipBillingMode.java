package com.fitnesstraining.memberships.model;

public enum MembershipBillingMode {
    /** Crea un pago PENDING por el precio del plan (vence hoy). */
    PENDING,
    /** Crea un pago PAID por el precio del plan. */
    PAID,
    /** No genera cobro (cortesía / ya liquidado fuera del sistema). */
    COMPLIMENTARY
}
