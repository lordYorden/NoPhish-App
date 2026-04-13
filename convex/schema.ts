import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
    members: defineTable({
        name: v.string(),
        familyRole: v.string(),
        userId: v.string(),
        avatarUrl: v.optional(v.string())
    }).index("byUserId", ["userId"]),

    tasks: defineTable({
        text: v.string(),
        isCompleted: v.boolean(),
    }),
})