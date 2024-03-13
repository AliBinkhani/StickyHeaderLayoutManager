
1.Support use in ConcatAdapter and ConcatAdapter nested ConcatAdapter

2.Supoort any custom Adapter withought extends XXXSticky

2.Fixed findLastVisiablexxx method

Please call setStickyHeaderProvider：

`

    stickyHeadersLinearLayoutManager.setStickyHeaderProvider(new StickyHeaderProvider() {
        @Override
        public boolean isStickyHeader(RecyclerView.Adapter<?> adapter, int position) {
            //use brv example
            return adapter instanceof BindingAdapter && ((BindingAdapter)adapter).getModel(position) instanceof ItemHover && ((ItemHover)((BindingAdapter)adapter).getModel(position)).getItemHover();
        }
    });
`

What is ConcatAdapter nested ConcatAdapter? 

like this:

`

    recyclerView.adapter = ConcatAdapter().apply {
        addAdapter(adapterA)
        addAdapter(ConcatAdapter().apply {
            addAdapter(adapterAA)
            addAdapter(ConcatAdapter().apply {
               addAdapter(adapterAAA)
               addAdapter(adapterBBB)
               addAdapter(adapterCCC)
            })
            addAdapter(adapterBB)
        })
        addAdapter(adapterB)
    }
`

