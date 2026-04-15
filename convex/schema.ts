import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
    members: defineTable({
        name: v.string(),
        familyRole: v.string(),
        userId: v.string(),
        isConnected: v.boolean(),
        avatarUrl: v.optional(v.string())
    }).index("byUserId", ["userId"]),

    tasks: defineTable({
        text: v.string(),
        isCompleted: v.boolean(),
    }),

    otpCodes: defineTable({
        code: v.string(),
        expiresAt: v.number(),
        issuerId: v.string(),
        memberId: v.optional(v.string()),
        circleId: v.string(),
    })
        .index("byCircleId", ["circleId"])
        .index("byExpiration", ["expiresAt"])
        .index("byIssuerAndExpiresAt", ["issuerId", "expiresAt"]),

    cricle: defineTable({
        name: v.string(),
        description: v.optional(v.string()),
        ownerId: v.string(),
    }),
})