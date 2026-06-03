(async () => {
    const storyBaseURL = "https://ongeki-net.com/ongeki-mobile/record/storyDetail/?story=";
    const memoryBaseURL = "https://ongeki-net.com/ongeki-mobile/record/memoryChapterDetail/?idx=";
    const shizukuBaseURL = "https://ongeki-net.com/ongeki-mobile/record/";
    const storyIDs = [1, 2, 3, 4, 5];
    const memoryIDs = [{ id: 70001, name: "Spring Memory" }, { id: 70002, name: "Summer Memory" }, { id: 70003, name: "Autumn Memory" }, { id: 70004, name: "Winter Memory" }, { id: 70005, name: "O.N.G.E.K.I. Memory" }, { id: 70099, name: "END CHAPTER" }];
    let results = new Array(storyIDs.length + memoryIDs.length + 1);
    let promises = [];
    const fetchJewelCount = (url, index, label) => {
        return fetch(url, { credentials: 'include' }).then(res => res.text()).then(html => {
            const doc = new DOMParser().parseFromString(html, "text/html");
            const jewelElement = doc.querySelector('.story_jewel_block span, .memory_jewel_block span');
            results[index] = label + ": " + (jewelElement ? jewelElement.innerText.trim() + " ジュエル" : "解放されていません");
        }).catch(() => { results[index] = label + ": 取得エラー"; });
    };
    storyIDs.forEach((id, i) => promises.push(fetchJewelCount(storyBaseURL + id, i, "ストーリー第" + id + "章")));
    memoryIDs.forEach((m, i) => promises.push(fetchJewelCount(memoryBaseURL + m.id, storyIDs.length + i, m.name)));
    promises.push(fetch(shizukuBaseURL, { credentials: 'include' }).then(r => r.text()).then(html => {
        const doc = new DOMParser().parseFromString(html, "text/html");
        const shizuku = doc.querySelector('.medal_block.f_l.t_r .v_m.p_3.f_14.gray');
        results[storyIDs.length + memoryIDs.length] = "しずく: " + (shizuku ? shizuku.innerText.trim() : "情報なし");
    }).catch(() => { results[storyIDs.length + memoryIDs.length] = "しずく: 取得エラー"; }));
    await Promise.all(promises);
    if (window.AndroidBridge && window.AndroidBridge.sendJewelResults) { window.AndroidBridge.sendJewelResults(JSON.stringify(results)); }
})();