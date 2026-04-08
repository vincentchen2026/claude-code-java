/**
 * Message type hierarchy for Claude Code.
 * <p>
 * Uses sealed interfaces and records to model the message types
 * corresponding to the TypeScript version's union types.
 * <p>
 * Key types:
 * <ul>
 *   <li>{@link com.claudecode.core.message.Message} — sealed interface for all message types</li>
 *   <li>{@link com.claudecode.core.message.ContentBlock} — sealed interface for content blocks</li>
 *   <li>{@link com.claudecode.core.message.SDKMessage} — sealed interface for SDK output messages</li>
 *   <li>{@link com.claudecode.core.message.Usage} — token usage statistics</li>
 * </ul>
 */
package com.claudecode.core.message;
