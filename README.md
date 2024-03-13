
1.Support use in ConcatAdapter and ConcatAdapter nested ConcatAdapter

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

    recyclerView.adapter = ConcatAdapter(ConcatAdapter.Config.Builder().setIsolateViewTypes(false).build()).apply {
        addAdapter(BindingAdapter().apply {
            addType<String>(R.layout.common_dialog_normal)
            models = arrayListOf("","","")
        })
        addAdapter(ConcatAdapter(ConcatAdapter.Config.Builder().setIsolateViewTypes(false).build()).apply {
            addAdapter(matchAdapter)
            addAdapter(loadStateAdapter)
        })
        addAdapter(BindingAdapter().apply {
            addType<String>(R.layout.coupon_type_item)
            models = arrayListOf("","","","","")
        })
    }
`

