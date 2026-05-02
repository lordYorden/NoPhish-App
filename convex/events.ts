import { v } from "convex/values";
import { mutation, query } from "./_generated/server";
import { paginationOptsValidator } from "convex/server";

export const register = mutation({
  args: {
    timestamp: v.number(),
    action: v.string(),
    moreDetails: v.optional(
      v.object({
        body: v.string(),
        packageName: v.string(),
        urls: v.array(v.string()),
      }),
    ),
  },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) {
      throw new Error("Unauthenticated");
    }

    ctx.db.insert("event", {
      userId: identity.subject,
      timestamp: args.timestamp,
      action: args.action,
      moreDetails: args.moreDetails,
    });
  },
});

export const get = mutation({
  args: {
    startTime: v.optional(v.number()),
    paginationOpts: paginationOptsValidator,
  },
  handler: async (ctx, args) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) {
      throw new Error("Unauthenticated");
    }

    const eventsQuery = ctx.db.query("event").withIndex("byDate");
    const filteredEventsQuery =
      args.startTime !== undefined
        ? eventsQuery.filter((q) =>
            q.and(
              q.eq(q.field("userId"), identity.subject),
              q.gte(q.field("timestamp"), args.startTime ?? 0),
            ),
          )
        : eventsQuery.filter((q) => q.eq(q.field("userId"), identity.subject));

    return await filteredEventsQuery.order("desc").paginate(args.paginationOpts);
  },
});


const get_by_circle = query({
  args: {
    circleId: v.string(),
    startTime: v.optional(v.number()),
    endTime: v.optional(v.number()),
    paginationOpts: paginationOptsValidator,
  },
  handler: async (ctx) => {
    const identity = await ctx.auth.getUserIdentity();
    if (!identity) {
      throw new Error("Unauthenticated");
    }

    //TODO: implament this query to get events by circleId, startTime and endTime
  },
});
