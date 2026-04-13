import { v } from "convex/values";
import { mutation, query } from "./_generated/server";

export const register = mutation({
	args: {
		name: v.string(),
		familyRole: v.string(),
		avatarUrl: v.optional(v.string()),
	},
	handler: async (ctx, args) => {
        console.log("hi test test");

		const identity = await ctx.auth.getUserIdentity();
		if (!identity) {
			throw new Error("Unauthenticated");
		}

		const existingMember = await ctx.db
			.query("members")
			.filter((q) => q.eq(q.field("userId"), identity.subject))
			.first();

		if (existingMember) {
			const patchData = {
				name: args.name,
				familyRole: args.familyRole,
				...(args.avatarUrl !== undefined ? { avatarUrl: args.avatarUrl } : {}),
			};

			await ctx.db.patch(existingMember._id, {
				...patchData,
			});
			return existingMember._id;
		}

		const insertData = {
			name: args.name,
			familyRole: args.familyRole,
			userId: identity.subject,
			...(args.avatarUrl !== undefined ? { avatarUrl: args.avatarUrl } : {}),
		};

		return await ctx.db.insert("members", insertData);
	},
});

export const get = query({
	args: {},
	handler: async (ctx) => {
		const identity = await ctx.auth.getUserIdentity();
		// if (!identity) {
		// 	return null;
		// }

		return await ctx.db
			.query("members")
            .collect();
			// .filter((q) => q.eq(q.field("userId"), identity.subject))
			// .first();
	},
});

export const getMemberById = query({
	args: {
		userId: v.string(),
	},
	handler: async (ctx, args) => {
		return await ctx.db
			.query("members")
			.filter((q) => q.eq(q.field("userId"), args.userId))
			.first();
	},
});
