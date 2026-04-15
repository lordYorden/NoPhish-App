import { v } from "convex/values";
import { mutation, query } from "./_generated/server";
import { redeem } from "./otps";
import { Doc } from "./_generated/dataModel";

//register a member to a circle using an OTP code
export const register = mutation({
	args: {
		code: v.string(),
		name: v.string(),
		familyRole: v.string(),
		avatarUrl: v.optional(v.string()),
	},
	handler: async (ctx, args) => {

		const identity = await ctx.auth.getUserIdentity();
		if (!identity) {
			throw new Error("Unauthenticated");
		}
		
		//run redeem mutation to validate OTP code
		const circleId = await redeem(ctx, args.code, identity.subject);
		
		const existingMember = await ctx.db
			.query("members")
			.filter((q) => q.eq(q.field("userId"), identity.subject))
			.first();

		const memberData = {
			name: args.name,
			familyRole: args.familyRole,
			userId: identity.subject,
			avatarUrl: args.avatarUrl ?? existingMember?.avatarUrl,
			isConnected: false,
		} satisfies Omit<Doc<"members">, "_id" | "_creationTime">;

		if (existingMember) {
			await ctx.db.patch(existingMember._id, {
				...memberData,
			});
		}else {
			await ctx.db.insert("members", memberData);
		}

		return circleId;
	},
});

//get members of a circle
export const get = query({
	args: {
		circleId: v.string(),
	},
	handler: async (ctx, args) => {
		const otpMembers = await ctx.db
			.query("otpCodes")
			.withIndex("byCircleId", (q) => q.eq("circleId", args.circleId))
			.collect();

		const memberIds = [...new Set(
			otpMembers
				.map((otp) => otp.memberId)
				.filter((memberId): memberId is string => Boolean(memberId)),
		)];

		const members = await Promise.all(
			memberIds.map(async (memberId) => {
				return await ctx.db
					.query("members")
					.withIndex("byUserId", (q) => q.eq("userId", memberId))
					.first();
			}),
		);

		return members.filter((member): member is NonNullable<typeof member> => Boolean(member));
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
