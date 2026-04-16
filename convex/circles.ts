import { v } from "convex/values";
import { mutation, query } from "./_generated/server";
import { Id } from "./_generated/dataModel";

const CIRCLE_TEMP_ID = "test_circle"

export const create = mutation({
    args: {
        name: v.string(),
        description: v.optional(v.string()),
    },
    handler: async (ctx, args) => {
        const identity = await ctx.auth.getUserIdentity();
        if (!identity) {
            throw new Error("Unauthenticated");
        }

        return await ctx.db.insert("cricle", {
            name: args.name,
            description: args.description,
            ownerId: identity.subject,
        });
    }
})

export const get = query({
    args: {
        circleId: v.string(),
    },
    handler: async (ctx, args) => {
        // const identity = await ctx.auth.getUserIdentity();
        // if (!identity) {
        //     throw new Error("Unauthenticated");
        // }

        const circle = await ctx.db.get("cricle", args.circleId as Id<"cricle">);

        if (!circle) {
            throw new Error("Circle not found");
        }

        return circle;
}})


export const get_my_circles = mutation({
    args: {},
    handler: async (ctx) => {
        const identity = await ctx.auth.getUserIdentity();
        if (!identity) {
            throw new Error("Unauthenticated");
        }

        const circle = await ctx.db.query("cricle")
            .filter((q) => q.eq(q.field("ownerId"), identity.subject))
            .first();

        return circle?._id ?? CIRCLE_TEMP_ID;
    }
})
