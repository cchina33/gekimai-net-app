(async () => {
    const storyBaseURL = "https://ongeki-net.com/ongeki-mobile/record/storyDetail/?story=";
    const memoryBaseURL = "https://ongeki-net.com/ongeki-mobile/record/memoryChapterDetail/?idx=";
    const storyIDs = [1, 2, 3, 4, 5];
    const memoryIDs = [70001, 70002, 70003, 70004, 70005, 70099];

    let totalJewels = 0;
    const fetchJewels = async (url) => {
        try {
            const res = await fetch(url, { credentials: 'include' });
            const html = await res.text();
            const doc = new DOMParser().parseFromString(html, "text/html");
            const jewelElement = doc.querySelector('.story_jewel_block span, .memory_jewel_block span');
            if (jewelElement) {
                const count = parseInt(jewelElement.innerText.trim().replace(/,/g, '')) || 0;
                totalJewels += count;
            }
        } catch (e) {
            console.error("Fetch error:", e);
        }
    };

    const promises = [];
    storyIDs.forEach(id => promises.push(fetchJewels(storyBaseURL + id)));
    memoryIDs.forEach(id => promises.push(fetchJewels(memoryBaseURL + id)));

    await Promise.all(promises);

    if (window.AndroidBridge && window.AndroidBridge.receiveJewelsForCalc) {
        window.AndroidBridge.receiveJewelsForCalc(totalJewels);
    }
})();
