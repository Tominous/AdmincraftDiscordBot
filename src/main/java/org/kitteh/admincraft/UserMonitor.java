/*
 * * Copyright (C) 2018 Matt Baxter http://kitteh.org
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.kitteh.admincraft;

public class UserMonitor {
    private long joinTime;
    private int mentions;
    private int messages;
    private boolean flagged;

    public UserMonitor() {
        this.joinTime = System.currentTimeMillis();
    }

    public int getMinutes() {
        return (int) (System.currentTimeMillis() - this.joinTime) / 1000 / 60;
    }

    public int getMentions() {
        return this.mentions;
    }

    public int getMessages() {
        return messages;
    }

    public int addMention(int count) {
        this.mentions += count;
        return this.mentions;
    }

    public int addMessage() {
        return ++this.messages;
    }

    public boolean needsFlag() {
        if (!this.flagged && this.messages > 2 && ((double) this.mentions / (double) this.messages) > 0.5) {
            this.flagged = true;
            return true;
        }
        return false;
    }

    public boolean canRemove() {
        return this.getMinutes() > 300;
    }
}
