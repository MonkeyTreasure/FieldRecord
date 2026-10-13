import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Shared Header Component - Material3 AppBar with energetic natural style
class AgriOpsHeader extends StatelessWidget implements PreferredSizeWidget {
  final String? title;
  final List<Widget>? actions;
  final bool showSearch;
  final bool showFilter;
  final VoidCallback? onSearch;
  final VoidCallback? onFilter;

  const AgriOpsHeader({
    super.key,
    this.title,
    this.actions,
    this.showSearch = true,
    this.showFilter = true,
    this.onSearch,
    this.onFilter,
  });

  @override
  Size get preferredSize => const Size.fromHeight(88);

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return AppBar(
      automaticallyImplyLeading: false,
      elevation: 0,
      toolbarHeight: 88,
      titleSpacing: 16,
      flexibleSpace: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            colors: [cs.primary, cs.primaryContainer],
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
        ),
      ),
      title: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Row(
            children: [
              Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  color: cs.onPrimary,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(Icons.agriculture, color: cs.primary, size: 20),
              ),
              const SizedBox(width: 10),
              Text(
                'AgriOps',
                style: GoogleFonts.inter(
                  color: cs.onPrimary,
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 0.5,
                ),
              ),
            ],
          ),
          const SizedBox(height: 2),
          Text(
            'Your Field. Your Tasks. Your Record.',
            style: GoogleFonts.inter(
              color: cs.onPrimary.withOpacity(0.7),
              fontSize: 11,
              letterSpacing: 0.3,
            ),
          ),
        ],
      ),
      actions: [
        if (showSearch)
          IconButton(
            icon: Icon(Icons.search, color: cs.onPrimary),
            onPressed: onSearch ?? () {},
            tooltip: 'Search',
          ),
        if (showFilter)
          IconButton(
            icon: Icon(Icons.filter_list, color: cs.onPrimary),
            onPressed: onFilter ?? () {},
            tooltip: 'Filter',
          ),
        if (actions != null) ...actions!,
      ],
    );
  }
}

/// Shared Bottom Navigation - 390×68 px
class AgriOpsBottomNav extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;

  const AgriOpsBottomNav({
    super.key,
    required this.currentIndex,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 68,
      decoration: BoxDecoration(
        color: const Color(0xFF162216),
        border: Border(
          top: BorderSide(color: Colors.green.withOpacity(0.2), width: 1),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceAround,
        children: [
          _buildNavItem(0, Icons.home, 'Home'),
          _buildNavItem(1, Icons.task_alt, 'Tasks'),
          _buildNavItem(2, Icons.photo_library, 'Gallery'),
          _buildNavItem(3, Icons.analytics, 'Reports'),
          _buildNavItem(4, Icons.person, 'Profile'),
        ],
      ),
    );
  }

  Widget _buildNavItem(int index, IconData icon, String label) {
    final isSelected = currentIndex == index;
    return Expanded(
      child: InkWell(
        onTap: () => onTap(index),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              icon,
              color: isSelected ? Colors.greenAccent : Colors.white54,
              size: 24,
            ),
            const SizedBox(height: 2),
            Text(
              label,
              style: TextStyle(
                color: isSelected ? Colors.greenAccent : Colors.white54,
                fontSize: 10,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Standard card style for consistent content treatment
class AgriOpsCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry? padding;
  final VoidCallback? onTap;

  const AgriOpsCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(16),
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF1E2E1E),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.green.withOpacity(0.2)),
      ),
      child: onTap != null
          ? InkWell(
              onTap: onTap,
              borderRadius: BorderRadius.circular(12),
              child: Padding(padding: padding!, child: child),
            )
          : Padding(padding: padding!, child: child),
    );
  }
}

/// Section header for lists
class AgriOpsSectionHeader extends StatelessWidget {
  final String title;
  final int count;
  final Color color;
  final VoidCallback? onSeeAll;

  const AgriOpsSectionHeader({
    super.key,
    required this.title,
    required this.count,
    required this.color,
    this.onSeeAll,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            '$title ($count)',
            style: TextStyle(
              color: color,
              fontWeight: FontWeight.w600,
              fontSize: 13,
            ),
          ),
          if (onSeeAll != null)
            TextButton(
              onPressed: onSeeAll,
              child: const Text(
                'Lihat Semua',
                style: TextStyle(color: Colors.white54, fontSize: 12),
              ),
            ),
        ],
      ),
    );
  }
}