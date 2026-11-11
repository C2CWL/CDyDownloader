package c2cw.cdydownload

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FollowStoreTest {
    private val author = DouyinAuthor("MS4wLjABAAAAexampleSecUid0001", "示例作者", "demo", avatar = "https://p3.douyinpic.com/a.jpeg", works = 3)

    @Test fun firstCheckWithoutBaselineMarksNothingNew() {
        assertEquals(emptyList<String>(), diffNewWorks(emptyList(), listOf("1", "2")))
        val entry = FollowedAuthor("s", "n").checked(listOf("3", "2", "1"), 10)
        assertEquals(0, entry.newCount)
        assertEquals(listOf("3", "2", "1"), entry.seenIds)
        assertEquals(10L, entry.lastChecked)
    }

    @Test fun newWorksAccumulateUntilViewed() {
        var entry = FollowedAuthor("s", "n", seenIds = listOf("2", "1"))
        entry = entry.checked(listOf("3", "2", "1"), 20)
        assertEquals(listOf("3"), entry.newIds)
        // A pinned old work at the top is not new.
        entry = entry.checked(listOf("1", "4", "3", "2"), 30)
        assertEquals(listOf("4", "3"), entry.newIds)
        entry = entry.viewed(listOf("4", "3", "2", "1"))
        assertEquals(0, entry.newCount)
        assertTrue(entry.seenIds.containsAll(listOf("1", "2", "3", "4")))
        assertEquals(0, entry.checked(listOf("4", "3"), 40).newCount)
    }

    @Test fun seenListIsBounded() {
        val many = (1..200).map { it.toString() }
        assertEquals(SEEN_LIMIT, FollowedAuthor("s", "n", seenIds = listOf("x")).viewed(many).seenIds.size)
    }

    @Test fun storePersistsFollowsAcrossInstances() {
        val dir = Files.createTempDirectory("follow").toFile()
        val file = File(dir, "follows.json")
        val store = FollowStore(file)
        assertFalse(store.isFollowed(author.secUid))
        store.follow(author, listOf("3", "2", "1"), now = 100)
        store.follow(author.copy(secUid = "MS4wLjABAAAAexampleSecUid0002", nickname = "第二位"), emptyList(), now = 200)
        store.update(author.secUid) { it.checked(listOf("4", "3"), 300) }
        val reopened = FollowStore(file)
        assertEquals(listOf("第二位", "示例作者"), reopened.authors.value.map { it.nickname })
        val first = reopened.get(author.secUid)!!
        assertEquals(listOf("4"), first.newIds)
        assertEquals("https://p3.douyinpic.com/a.jpeg", first.avatar)
        assertEquals(300L, first.lastChecked)
        reopened.unfollow(author.secUid)
        assertFalse(FollowStore(file).isFollowed(author.secUid))
        dir.deleteRecursively()
    }

    @Test fun corruptFileIsIgnored() {
        assertEquals(emptyList<FollowedAuthor>(), FollowCodec.decode("{not json"))
        assertEquals(emptyList<FollowedAuthor>(), FollowCodec.decode(null))
    }
}
