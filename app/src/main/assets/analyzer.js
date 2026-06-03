(function() {
    let _domtoimage = window.domtoimage;
    Object.defineProperty(window, 'domtoimage', {
        configurable: true,
        get: function() { return _domtoimage; },
        set: function(val) {
            _domtoimage = val;
            if (_domtoimage) {
                ['toPng', 'toJpeg'].forEach(method => {
                    if (_domtoimage[method]) {
                        let origMethod = _domtoimage[method];
                        _domtoimage[method] = function() {
                            return origMethod.apply(this, arguments).then(function(result) {
                                if (typeof result === 'string' && result.startsWith('data:image') && window.AndroidBridge) {
                                    window.AndroidBridge.showPreviewImage(result);
                                }
                                return result;
                            });
                        };
                    }
                });
            }
        }
    });

    let origOpen = window.open;
    window.open = function(url, name, features) {
        let newWin = origOpen.call(window, url, name, features);
        if (newWin) {
            try {
                let origWrite = newWin.document.write;
                newWin.document.write = function(content) {
                    let match = String(content).match(/data:image\/[a-zA-Z]+;base64,[^"'\s<>]+/);
                    if (match && window.AndroidBridge) {
                        window.AndroidBridge.showPreviewImage(match[0]);
                    }
                    origWrite.apply(newWin.document, arguments);
                };
            } catch(e) {}
        }
        return newWin;
    };

    const origClick = HTMLAnchorElement.prototype.click;
    HTMLAnchorElement.prototype.click = function() {
        if (this.download && this.href && this.href.startsWith('data:image') && window.AndroidBridge) {
            window.AndroidBridge.showPreviewImage(this.href);
            return;
        }
        origClick.call(this);
    };

    let s = document.createElement('script');
    s.setAttribute('type', 'text/javascript');
    s.setAttribute('src', 'https://sgimera.github.io/mai_RatingAnalyzer/maidx_tools.js');
    document.head.appendChild(s);
})();